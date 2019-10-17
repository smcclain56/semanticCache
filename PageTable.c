#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#include "Control.h"
#include "QueryData.h"
#include "PageTable.h"
/*
IDEA: PTE in a pageTableStart has a spot for the end column -- if the pageTableEnd[end] is not valid (not an exact match) we can use this space as like
TLB -- very fast (contains the query that was added most recently -- temporal locality
IDEA : the problem is that bits are becoming valid because a word_32 isnt' large enough so hold data that is sized word_32 and also additional bits
IDEA: Should I only use the TLB space on range queries -- because point queries are not difficult to get?
IDEA: Will I need some sort of modify or reference bit -- I don't think so because I'm never actually replacing anything

TODO: NEED A BIT WHICH DISTINGUISHES BETWEEN AND/OR IN PAGE TABLE
*/

/*
Prints out the pageTableStart given the total number of columns
*/
void print_startTb(int num_columns){
  int i, valid, endColumn;
  word_32 PTEstart, bitMask;
  //char hyphin = '-';
  printf("StartCol   |  V  |  EndCol\n");
  for(i=0; i<=num_columns; i++){
    PTEstart = pageTableStart[i]; //get entry
    valid = PTEstart >> BASE_LEN; //most sig bit

    if(valid==1){
      bitMask = (ONES >> (BASE_LEN-column_bits));
      endColumn = (PTEstart & bitMask); //least signficant bits
      printf("%d          |  %d  |  %d\n",i, valid, endColumn);
    }else{
      //printf("%d          |  %d  |  %c\n",i,valid,hyphin);
    }
  }
  printf("\n");
}

/*
Prints out the pageTableEnd of startColumn inputed, given the number of total columns
*/
void print_endTb(int startColumn, int num_columns){
  int i, valid;
  word_64 PTE, bitMask, data;
  //char hyphin = '-';
  printf("EndCol  |  V    |  DATA\n");
  for(i=startColumn; i<=num_columns; i++){
    PTE = pageTableEnd[startColumn][i]; //get entry
    valid = PTE >> PTE_LEN; //most sig bit

    if(valid==1){
      bitMask = (ONES >> (PTE_LEN-data_bits));
      data = (PTE & bitMask); //least signficant bits
        printf("%d       |  %d    |  %.16X\n",i, valid, data);
    }else{
      //printf("%d      |  %d    |  %c\n",i,valid,hyphin);
    }
  }
  printf("\n");
}

/*
Adds a query to the cache if it is not stored yet -- assumes pageTableEnd has already been made and that the query is not already present
@param
  int start -> start column of the query
  int end -> end column of query
  int num_columns -> number of columns in pageTableStart
  queryData *results -> results of query executed to be added to cache
*/
void addToCache(int start, int end, int num_columns, queryData *results){
  word_32 PTEstart;
  //word_64 PTEend;
  printf("adding [%d,%d]\n",start,end );
  //turn valid bit in pageTableStart to a 1
  PTEstart = VALID_BIT_32;

  //add end column to end of PTEstart
  pageTableStart[start] = (PTEstart | end);

  //change valid bit in pageTableEnd and add data to pageTableEnd -- because PTEend is word_64 and result->result is only word_32?
  word_64 result = *results->result;
  pageTableEnd[start][end] = (VALID_BIT_64| result); //TODO ERRORS

  //print_startTb(num_columns);
  print_endTb(start,num_columns);
}

/*
Gets the data from query[start,end] when it is assumed to be already in cache and returns it as word_32
@param
  int start -> start column of query
  int end -> end column of query
@return
  returns word_32 data
*/
word_32 getData(int start, int end){
  word_64 PTEend, bitMask, data;

  //get entry from pageTableEnd[start][end]
  PTEend = pageTableEnd[start][end];

  //seperate data bits
  bitMask = ONES >> (PTE_LEN - column_bits);
  data = (PTEend & bitMask);

  //return data
  return data;
}

/*
Checks if query[start,end] is stored in the cache
  int start -> start column of query
  int end -> end column of query
@return
  returns 1 if query[start,end] is in cache
  returns 0 if not in cache.
*/
int checkQuery(int start, int end, int num_columns){
  int validStart, validEnd;
  word_32 PTEstart;
  word_64 PTEend;

  //check if pageTableEnd exists yet
  if(tableExist[start] == -1){ //table doesn't exist yet
    tableExist[start] = 0; //update exists data
    pageTableEnd[start] = (word_64*) malloc((num_columns-start)*sizeof(word_64*)); //TODO SHOULD THIS BE WORD_64*
    return 0;
  }

  //check valid bit in pageTableStart[start]
  PTEstart = pageTableStart[start];
  validStart = PTEstart >> BASE_LEN;
  if (validStart == 1){
    //check valid bit in pageTableEnd
    PTEend = pageTableEnd[start][end];
    validEnd = PTEend >> PTE_LEN;
    if (validEnd == 1){ // if valid in end table then it is in cache
      return 1;
    }
  }
  return 0;
}

/*
Creates and initialies the pageTableStart and pageTableEnd
@param
  int num_columns -> number of total columns in bitmapFile
  int num_rows -> number of total rows in bitmapFile TODO HOW TO FIND THIS -- DOES IT NEED TO BE PARAM
*/
void init_pagetb(int num_columns){
  //malloc tables
  pageTableStart = (word_32*) malloc(num_columns*sizeof(word_32));
  pageTableEnd = (word_64**) malloc(sizeof(word_64) * num_columns);
  //malloc tableExist to show whether a pageTableEnd exists for that start column
  tableExist = (int*) malloc(sizeof(int)*num_columns);//how many words are in each column (empty --> -1)
  int currentCol = 0;
  //fill tableExist[] so that no columns pageTableEnd have been made
  while(currentCol <= num_columns){
    tableExist[currentCol] = -1;
    currentCol++;
  }
  //calculate number of bits
  column_bits = (int) ceil(log10(num_columns) / log10(2));
  data_bits = 8*sizeof(word_32);

  //fill pageTableStart
  int i;
  for(i=0; i<num_columns; i++){
    pageTableStart[i] = 0;
  }
}

/*
Frees the memory of the pageTables and supporting arrays
@param
  int num_columns -> number of columns in bitmapFile
*/
void freeCache(int num_columns){
  printf("freeing\n");
  int i;
  for(i =0; i<=num_columns; i++){
    tableExist[i] = -1;
  }
  free(tableExist);
  free(pageTableStart);
  free(pageTableEnd);
}
