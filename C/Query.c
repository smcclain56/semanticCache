#include <stdio.h>
#include <string.h>
#include <math.h>

#include "Query.h"
#include "Control.h"
#include "SegUtil.h"
#include "Vars.h"
#include "WAHQuery.h"
#include "VALQuery.h"
#include "RawBitmapReader.h"
#include "Core.h"
#include "PageTable.h"
#include "Hashmap.h"

extern word_32 TESTING;

/*
TODO: DOES EXECUTERANGE NEED A PARAM AS A DESINATION FOR THE RESULTS? WOULD HAVE TO CHANGE MALLOC STUFF
*/
//word_32 **cols;//loaded columns
// int num_columns;
//int *sz;//size of each loaded column (init at -1 --> not loaded yet)
char query_path[BUFF_SIZE];//path for results of queries
//unsigned int maxWords;

/**
 * Runs the designated file of queries on the bitmap file folder
 char *folder bitmap file folder
 char *query file to query
 */
 void runQueries(char *bitmapFile, char *queryFile)
 {
    //open the query file for reading
    FILE *fp = fopen(queryFile, "r");
    if(fp == NULL) {
       fprintf(stderr,"Can't open query file\n");
    }
		else { //if file is not null then start reading the contents
      int num_columns = getNumCols(bitmapFile, queryFile);
      word_32 **cols = (word_32 **) malloc(sizeof(word_32* ) * num_columns);//the actual column
      //int *sz = (int *) malloc(sizeof(int)*num_columns);//how many words are in each column (empty --> -1)
      int sz[num_columns];
      initSize(sz, num_columns);
      struct list **hash = NULL; //hash table

      int column_bits;
      //initialize hash table
      if(CACHE_POLICY == HASH_MAP){
        column_bits = (int) ceil(log10(num_columns) / log10(2));
        hash = createTable();
      }
    	int start, end, num_ranges = 0, size, i,  j, num_toks, lineNum = 0,k;
    	char str[LSIZE];
			char copy[LSIZE];
      unsigned int hash_key;
      char queryTokens[MAX_NUM_RANGES][MAX_NUM_CHAR];
      queryData* results[MAX_NUM_RANGES];
      //buildQueryOutput(queryFile,num_columns);
      for(i=0; i<MAX_NUM_RANGES; i++){
        results[i] = (queryData*) malloc(sizeof(queryData));
      }
      //printCol(bitmapFile,8, cols, sz);
      //char buff[BUFF_SIZE];
      //snprintf(buff,sizeof(buff),"%s%s",query_path,".dat");//build the output file name
    	//get each line
    	while(fgets(str, LSIZE, fp) != NULL){
      	if(!strncmp(str,"#",1)){ //if it starts with # then it's a comment so continue
          lineNum++;
          continue;
      	}
        // if(lineNum == 50){
        //   printHashTable(hash,column_bits);
        //   exit(0);
        // }
      	strcpy(copy, str);
      	num_toks = tokenize(copy, queryTokens);

    		//calculate num_ranges
    		i = 0;
  			num_ranges = 0;
    		size = 0;
    		while(i < num_toks){
        	size++;
        	i++;
      	}
      	num_ranges = (int) ceil(size/3.0);
        //printf("num_ranges = %d\n",num_ranges );
        //  printHashTable(hash,column_bits);
        //clear results
        for(j = 0; j < num_ranges; j++){
          results[j]->result = NULL;
          results[j]->resultSize = 0;
          results[j]->complete_cache = 0;
        }
				//execute each range by ORing its columns or get result from cache
				i = j = 0;
        //printf("lineNum = %d\n",lineNum );
				while(j < num_ranges){
					start = (int) atoi(queryTokens[i]);
					end = (int) atoi(queryTokens[i+1]);
          //printf("[%d,%d]\n",start,end );

          //check cache
          if(CACHE_POLICY == HASH_MAP) {
            hash_key = hash_key_builder(start, end, "OR", column_bits); //TODO: THIS NEEDS TO BE AND/OR DEPENDING ON QUERY
            cache_search(results[j], bitmapFile, start, end, hash_key, hash, cols, sz, column_bits);
          }
          else { //no cache
            setupColumns(results[j], bitmapFile, start, end, cols, sz);
            executeRange(results[j], bitmapFile, start, end, hash, cols, sz);


          }

					i+=3;
					j++;
				}
				//AND or OR the ranges all together into results[0]
				i=1; j=2;
				while(i < num_ranges) {
					if(num_ranges == 1){ //if only one range then it's already stored in results[0]
            break;
					}
					if(strcmp(queryTokens[j],"&") == 0){
            AND_WAH(results[0]->result, results[0]->result, results[0]->resultSize, results[i]->result, results[i]->resultSize);
          }
					else if(strcmp(queryTokens[j],"|") == 0){
						OR_WAH(results[0]->result, results[0]->result, results[0]->resultSize, results[i]->result, results[i]->resultSize);
          }

					i++;
					j+=3;
				}
        lineNum++;
  		}

      if(CACHE_POLICY == HASH_MAP){
        hashmap_destroy(hash);
      }

      //free memory
      for(k=0; k<MAX_NUM_RANGES; k++){
        if (results[k] != NULL) {
          free(results[k]);
          results[k] = NULL;
        }
      }
      //free all bitmaps
      //freeCol(cols, num_columns);
  	}
}

void cache_search(queryData *ret, char *bitmapFile, int start, int end, unsigned int key, struct list **hash, word_32 **cols, int *sz, int column_bits){
  //initialize end_stored to end
  int start_stored = start;
  int end_stored = end;

  //call set up columns to load appropiate columns and malloc results->result
  setupColumns(ret, bitmapFile, start, end, cols, sz);
  // use appropiate lookup algorithm
  if(LOOKUP == JARVIS){
    // end_stored = hash_lookup(ret, hash, start, start, end, key, column_bits);
     unsigned int found_key = jarvis_lookup(ret, hash, start, end, key, column_bits);
     end_stored = getEndCol(found_key, column_bits);
     start_stored = getStartCol(found_key,column_bits);

  }else{
    end_stored = hash_lookup(ret, hash, start, start, end, key, column_bits);
  }

  //find remainder query
  ret->result = getRemainderQuery(ret, bitmapFile, start_stored, end_stored, start, end, hash, cols, sz, column_bits);
}
/*
Checks what pieces of query were found during lookup and adds or removes missing/extra pieces
to results if needed. Executes query if none of the query was cached. Adds the full query
found to cache if was not already stored.
@param
  queryData *results -> results from most similar query found in cache
  char *bitmapFile -> bitmapFile being used for querying
  int start_stored -> starting column of query most similar to desired query
  int end_stored -> ending column of query most similar to desired query
  int start_wanted -> starting column of query being looked up
  int end_wanted -> ending column of query being looked up
@return
  word_32* --> results of full query [start_wanted, end_wanted]
*/
word_32* getRemainderQuery(queryData *results, char *bitmapFile, int start_stored, int end_stored, int start_wanted, int end_wanted, struct list **hash, word_32 **cols, int *sz, int column_bits){

  //The exact query was cached
  if(start_stored == start_wanted && end_stored == end_wanted){
    if(results->complete_cache == 0){ //this was made from lookup so not cached yet
      hash_insert(hash, start_wanted, end_wanted, results->result,column_bits);
      results->complete_cache = 1;
    }
    return results->result;
  }

  //none of query was cached
  else if(end_stored < start_wanted){
    // printf("none stored -- execute range\n" );
    executeRange(results, bitmapFile, start_wanted, end_wanted, hash, cols, sz);
  }

  //need to chop or add pieces to results
  else if(start_wanted < start_stored && end_wanted < end_stored ){
    //printf("1.\n" );
    results = chop_endW_to_endS(results, end_stored, end_wanted, start_wanted, hash, cols, sz);
    results = add_startW_to_startS(results, start_wanted, start_stored, hash, cols, sz);
  }else if(start_wanted == start_stored && end_wanted < end_stored){
    //printf("2.\n" );
    results = chop_endW_to_endS(results, end_stored, end_wanted, start_wanted, hash, cols, sz);
  }else if(start_stored < end_wanted && end_stored == end_wanted ){
    //printf("3.\n" );
    results = chop_startS_to_startW(results, start_stored, start_wanted, end_wanted, hash, cols, sz);
    results = chop_endW_to_endS(results, end_stored, end_wanted, start_wanted, hash, cols, sz);
  }else if(start_stored < start_wanted && end_wanted<end_stored ){
    //printf("4.\n" );
    results = chop_startS_to_startW(results, start_stored, start_wanted, end_wanted, hash, cols, sz);
  }else if(start_stored < start_wanted && end_wanted > end_stored ){
    //printf("5.\n" );
    results = chop_startS_to_startW(results, start_stored, start_wanted, end_wanted, hash, cols, sz);
    results = add_endS_to_endW(results, end_stored, end_wanted, hash, cols, sz);
  }else if(start_wanted < start_stored && end_wanted > end_stored){
    //printf("6.\n" );
    results = add_startW_to_startS(results, start_wanted, start_stored, hash, cols, sz);
    results = add_endS_to_endW(results, end_stored, end_wanted, hash, cols, sz);

  }

  //all columns from cache are wanted, plus additional ones
  else if((start_wanted<=start_stored && end_wanted==start_stored) || (end_stored == end_wanted && start_wanted < start_stored)){
    //printf("7.\n" );
    results = add_startW_to_startS(results, start_wanted, start_stored, hash, cols, sz);
  }else if( (end_wanted > end_stored && end_stored == start_wanted) || (start_wanted == start_stored && end_stored < end_wanted)){
    //printf("8.\n" );
    results = add_endS_to_endW(results, end_stored, end_wanted, hash, cols, sz);
  }

  //add new query to cache
  hash_insert(hash, start_wanted, end_wanted, results->result, column_bits);
  //printf("inserted\n");
  results->complete_cache = 1;

  return results->result;
}
/*
Adds columns from start_wanted to start_stored/end_wanted (they're equal)
*/
queryData* add_startW_to_startS(queryData *results, int start_wanted, int start_stored, struct list **hash, word_32 **cols, int *sz){
  int i;
  for(i=start_wanted; i<start_stored; i++){
    OR_WAH(results->result, results->result, results->resultSize, cols[i], sz[i]); //TODO UNCOMMENT
  }
  return results;
}

/*
Adds columns from end_stored/start_wanted to end_wanted
*/
queryData* add_endS_to_endW(queryData* results, int end_stored, int end_wanted, struct list **hash, word_32 **cols, int *sz){
  int i;
  for(i=end_stored; i<end_wanted; i++){
    OR_WAH(results->result, results->result, results->resultSize, cols[i], sz[i]);
  }
  return results;
}
/*
Gets rid of columns between end_wanted and end stored
*/
queryData* chop_endW_to_endS(queryData *results, int end_stored, int end_wanted, int start_wanted, struct list **hash, word_32 **cols, int *sz){

  // get maxWords before malloc
  int i;
  unsigned int maxWords = 0;
  for(i=start_wanted; i<=end_wanted; i++){
    if(sz[i]>maxWords){
      maxWords = sz[i];
    }
  }
  //printf("start_wanted = %d, end_wanted=%d\n",start_wanted,end_wanted );

  // malloc queryData
  int numColsWanted = (end_wanted-start_wanted)+1;

  queryData **queryResults = (queryData**) malloc(sizeof(queryData)*numColsWanted); //NOTE THIS IS CAUSING TROUBLE

  for(i=0; i<numColsWanted; i++){
    queryResults[i] = (queryData*) malloc(sizeof(queryData));
    queryResults[i]->result = (word_32*) malloc(sizeof(word_32*)*maxWords);
    queryResults[i]->resultSize = maxWords;

  }
  // chop off pieces between end_wanted and end_stored
  int j=0;
  for(i=start_wanted; i<=end_wanted; i++){
    AND_WAH(queryResults[j]->result, results->result, results->resultSize, cols[i], sz[i]);
    j++;
  }

  // if it was only one column wanted then return queryResults[0]
  if(numColsWanted == 1){
    return queryResults[0];
  }
  // OR all the results back together
  for(i=1; i<numColsWanted; i++){
    OR_WAH(queryResults[0]->result, queryResults[0]->result, queryResults[0]->resultSize, queryResults[i]->result, queryResults[i]->resultSize);
  }

  return queryResults[0];
}

/*
Gets rid of columns between start_stored and start_wanted
*/
queryData* chop_startS_to_startW(queryData *results, int start_stored, int start_wanted, int end_wanted, struct list **hash, word_32 **cols, int *sz){
  // get maxWords before malloc
  int i;
  unsigned int maxWords = 0;
  for(i=start_wanted; i<=end_wanted; i++){
    if(sz[i]>maxWords){
      maxWords = sz[i];
    }
  }


  // malloc queryData
  int numColsWanted = (end_wanted-start_wanted)+1;

  queryData **queryResults = (queryData**) malloc(sizeof(queryData)*numColsWanted);
  for(i=0; i<numColsWanted; i++){
    queryResults[i] = (queryData*) malloc(sizeof(queryData));
    queryResults[i]->result = (word_32*) malloc(sizeof(word_32*)*maxWords);
    queryResults[i]->resultSize = maxWords;
  }

  //chop off pieces between end_wanted and end_stored
  int j=0;
  for(i=start_wanted; i<end_wanted; i++){
    AND_WAH(queryResults[j]->result, results->result, results->resultSize, cols[i], sz[i]);
    j++;
  }

  //if it was only one column wanted then return queryResults[0]
  if(numColsWanted == 1){
    return queryResults[0];
  }

  //OR all the results back together
  for(i=1; i<numColsWanted; i++){
    OR_WAH(queryResults[0]->result, queryResults[0]->result, queryResults[0]->resultSize, queryResults[i]->result, queryResults[i]->resultSize);
  }
  return queryResults[0];
}

/**
Tokenize a line of the query file by the brackets and commas
*/
int tokenize(char *line, char instruction[MAX_NUM_RANGES][MAX_NUM_CHAR]) {
  char delim[] = "[], \n";
  int index = 0;
  char *token = strtok(line, delim);
  while(token != NULL && index < MAX_NUM_RANGES){
      strcpy(instruction[index], token); //THIS LINE
      index++;
      token = strtok(NULL, delim);
  }
  return index;
}


void setupColumns(queryData *ret, char *bitmapFile, int start, int end, word_32 **cols, int *sz){
  //load in necessary columns from memory
  //printf("set up columns\n" );
  int i;
  unsigned int maxWords = 0;
	for(i=start; i<=end; i++){
		if(sz[i] == -1){ // column is not already loaded
			loadCol(bitmapFile, i, cols, sz);
		}
		//update maxWords if needed
		if(sz[i]>maxWords){
			maxWords = sz[i];
		}

	}
	// malloc results struct
	ret->result = (word_32 *) malloc(sizeof(word_32) * maxWords);
	ret->resultSize = maxWords;
  ret->complete_cache = 1;

}

void executeRange(queryData *ret, char *bitmapFile, int start, int end, struct list **hash, word_32 **cols, int *sz)
{

  if(CACHE_POLICY != HASH_MAP){
    setupColumns(ret, bitmapFile, start, end, cols, sz);
  }

  int i;
	//OR columns together
	if(start == end){ //point query
		ret->result = cols[start];
    return;
    //return results;
	}
	else{ //range query
		for(i=start; i<=end; i++){
			if(COMPRESSION == WAH){
				if(i==start){ //put first column in results
          ret->result = cols[i];  //DC: HERE IS A BIG PROBLEM (SEGFAULT)
				}else{ //OR each other column to
					OR_WAH(ret->result, ret->result, ret->resultSize, cols[i], sz[i]);
				}
			}else if(COMPRESSION == VAL){
				if(i==start){ //put first column in results
					ret->result = cols[i];
				}else{ //OR each other column to results
					OR_VAL(ret->result, ret->result, ret->resultSize, cols[i], sz[i]);
				}
			}
		}
	}

	//return results;
}



// /**
//  * Initializes environment (structs,paths,etc)
//  */
// void init(char *bitmapFile, char *queryFile, word_32 **cols, int *sz){
// 	num_columns = 0;
// 	//calculate number of columns
// 	while(1){
// 		char name[BUFF_SIZE];
// 		snprintf(name,sizeof(name),"%s%s%d%s",bitmapFile,"/col_",num_columns,".dat");//build the file name based on colNum
//
// 		//counting the number of columns there are in that folder
// 		if(access(name,F_OK) != -1){
// 			num_columns++;
// 		}
// 		else {
// 			break;
// 		}
// 	}
// 	//building the folder for query results
// 	//the results will be saved in a folder where the query file was
// 	//Ex. if we ran "Queries/query1.txt"
// 	//the results will be "Queries/QueryResults_query1.txt/qID_0.dat" etc
// 	//char folder[] = "QueryResults_";
//   /*
// 	char results_folder[BUFF_SIZE];
// 	snprintf(results_folder,BUFF_SIZE,"%s_RESULTS/",queryFile);
//
// 	mkdir(results_folder,S_IRWXU);
// 	int i;
// 	//for(i=0;i<BUFF_SIZE;query_path[i++]='\0');
//
// 	strcpy(query_path,results_folder);
// 	strcat(query_path,"/qID_");
//   */
//   snprintf(query_path, BUFF_SIZE, "%s_RESULTS.dat", queryFile);
//   //for(i=0;i<BUFF_SIZE;query_path[i++]='\0'); //TODO WHAT WAS THIS LINE SUPPOSED TO DO
//
// 	cols = (word_32 **) malloc(sizeof(word_32 *) * num_columns);//the actual column
// 	sz = (int *) malloc(sizeof(int)*num_columns);//how many words are in each column (empty --> -1)
// 	int currentCol = 0;
// 	//fill sz[] so that no columns have been loaded in
// 	while(currentCol <= num_columns){
// 		sz[currentCol] = -1;
// 		currentCol++;
// 	}
//
// 	initUtilSegs(WAH);
//   //return cols;
//
// }

void buildQueryOutput(char *queryFile, int num_columns){
  //building the folder for query results
  //the results will be saved in a folder where the query file was
  //Ex. if we ran "Queries/query1.txt"
  //the results will be "Queries/QueryResults_query1.txt/qID_0.dat" etc
  //char folder[] = "QueryResults_";

  char results_folder[BUFF_SIZE];
  snprintf(results_folder,BUFF_SIZE,"%s_RESULTS/",queryFile);

	mkdir(results_folder,S_IRWXU);

	//int i;
	//for(i=0;i<BUFF_SIZE;query_path[i++]='\0');

  strcpy(query_path,results_folder);
  strcat(query_path,"/qID_");

  snprintf(query_path, BUFF_SIZE, "%s_RESULTS.dat", queryFile);
  //for(i=0;i<BUFF_SIZE;query_path[i++]='\0'); //TODO WHAT WAS THIS LINE SUPPOSED TO DO

}

int getNumCols(char *bitmapFile, char *queryFile){
	int num_columns = 0;
	//calculate number of columns
	while(1){
		char name[BUFF_SIZE];
		snprintf(name,sizeof(name),"%s%s%d%s",bitmapFile,"/col_",num_columns,".dat");//build the file name based on colNum

		//counting the number of columns there are in that folder
		if(access(name,F_OK) != -1){
			num_columns++;
		}
		else {
			break;
		}
	}
  snprintf(query_path, BUFF_SIZE, "%s_RESULTS.dat", queryFile);
  initUtilSegs(WAH);
  return num_columns;
}

void initSize(int *sz, int num_columns){
  int currentCol = 0;

  //fill sz[] so that no columns have been loaded in
  while(currentCol <= num_columns){
    sz[currentCol] = -1;
    currentCol++;
  }
}




/**
 * Loads column into appropriate location in cols from global path
 */
void loadCol(char *bitmapFile, int column, word_32 **cols, int *sz){
	char file[BUFF_SIZE];
	//bitmap_out_gc.txt_UNSTRIPED_1_COMPRESSED/col_d.dat
  //printf("snprint\n" );
	snprintf(file,sizeof(file),"%s%s%d%s",bitmapFile,"/col_",column,".dat");//build the file name based on colNum

  //printf("open\n" );
	FILE *fp = fopen(file,"rb");
	if(fp==NULL){//out of range query probably
		printf("Can't open file--");
		return;
	}

	//just trying to find the size of the file (to know how many words are in it)
	struct stat st;
	stat(file, &st);
	sz[column] = (st.st_size/sizeof(word_32));//save number of words in column into sz array
	//fill in columns array
	cols[column] = (word_32 *) malloc(sizeof(word_32*) *sz[column]);

	fread(&(cols[column][0]),sizeof(word_32),sz[column],fp);//read in the column

  //fclose(fp); //TODO Why was this not working?
}

// void printCol(char *bitmapFile, int column, word_32 **cols, int *sz){
//   char file[BUFF_SIZE];
//   //snprintf(file,sizeof(file),"%s%s%d%s",bitmapFile,"/col_",column,".txt");//build the file name based on colNum
//   snprintf(file,sizeof(file),"%s%d%s","../../BitmapWorkloadGenerator/src/col_",column,".dat");
//   printf("file = %s\n",file );
//   FILE *fp = fopen(file,"wb+");
//   if(fp == NULL){
//     printf("can't open results file\n" );
//     return;
//   }
//
//   fwrite(cols[column], sizeof(word_32), sz[column], fp);
//   fclose(fp);
//
// }
void printCols(int column, word_32** cols, int *sz){
  int i;
  printf("cols[%d] = ",column);
  for(i=0; i<sz[column]; i++){
    printf("%.8X ",cols[column][i] );
  }
  printf("\n" );

}
void freeCol( word_32 **cols, int num_columns) {
  int i;
  for (i = 0; i < num_columns; i++) {
     printf("cols[%d] = %p\n",i,cols[i] );

    if (cols[i] != NULL /*&& i!=2*/){ //NOTE NOT FREEING COLS[2]
      free(cols[i]);
    }
    printf("freed\n" );
  }
  if (cols != NULL){
    free(cols);
  }
}
