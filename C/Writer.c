#include <stdio.h>
#include "Control.h"
#include "Writer.h"
#include "RawBitmapReader.h"

#include "Core.h"

int numCols;//number of columns
int max = 200;//maximum number of columns to build at once (cap on number of files open at once)

word_read *curr;//the current word we're building to each column
int word_count;//how many words in each column
FILE **col_files;//file pointers to all the column files
char *bitmap_file;
int iterations;

int reformat(char **file){
	bitmap_file = *file;
	printf("\nFile: %s\n",bitmap_file);

	// printf("\ninside reformat func\n");

	col_files = (FILE **) malloc(sizeof(FILE *) * max);//allocate file pointers
	curr = (word_read *) malloc(sizeof(word_read) * max);

	printf("\tUNSTRIPING BEGIN...");
	if(!toUnstriped()) return 0;
	printf("COMPLETE\n");

	int i;
	for(i=1;i<=MAX_NUM_THREADS;i++){
		printf("\tSTRIPING(%d/%d) BEGIN...",i,MAX_NUM_THREADS);
		toStriped(i);
		printf("COMPLETE\n");
	}
	free(col_files);
	free(curr);

	printf("\tCOMPLETE ALL\n");
	return 1;
}

/*
 * Takes the BITMAP_FILE and reformats to striped file folder
 */
int toStriped(int num_threads){
	//char *cols = toUnstriped();//get the file extension of the unstriped files

	int numWords = BLOCK_SIZE*1000/sizeof(word_read);//this is the number of words to load per thread, per block
	//numWords=numWords/num_threads;

	word_read buffer[numWords];
	int i;
	int col_counter=0;
	int file_counter = -1;
	char *col_folder = stripedExt(bitmap_file,num_threads);
	strcat(col_folder,"/");

	mkdir(col_folder,S_IRWXU);//make that directory
	char col_path[BUFF_SIZE];
	snprintf(col_path,BUFF_SIZE,"%s%s",col_folder,"col_");
	FILE **ptrs;//pointers to the unstriped files used to build the striped files
	ptrs = (FILE **) malloc(sizeof(FILE *) * num_threads);
	char *source_path = unstripedExt(bitmap_file);
	strcat(source_path,"/");
	while(1){
		for(i=0;i<num_threads;i++){//open an unstriped file for each stripe
			if(col_counter<numCols){//if that file exists
				char name[BUFF_SIZE];
				snprintf(name,BUFF_SIZE,"%scol_%d.dat",source_path,col_counter++);
				ptrs[i] = fopen(name,"rb");//try to open the file
				if(ptrs[i]==NULL){
					printf("Cannot open column file for striping\n%s\n",name);
					free(col_folder);
					return 0;
				}
			}
			else{
				break;
			}
		}

		file_counter+=i;
		char buff[BUFF_SIZE];
		snprintf(buff,BUFF_SIZE,"%s%d.dat",col_path,file_counter);//build the name of the column file for each column
		FILE *writeTo = fopen(buff,"wb");//open the striped file we're writing to

		int j=i;//keep track of how many columns we're reading (in case we're on the last one that has fewer columns)
		numWords = BLOCK_SIZE*1000/sizeof(word_read);//this is the number of words to load per thread, per block
		//numWords-=numWords%j;
		numWords=numWords/j;
		//printf("Striping to file %d, %d columns\n",file_counter,j);
		int words = numWords;
		while(1){
			for(i=0;i<j;i++){
//				if(num_threads==3 && file_counter==121){
//					printf("About to read %d words\n",numWords);
//					if(ptrs[i]==NULL) printf("Null file pointer\n");
//				}
				words = fread(buffer,sizeof(word_read),numWords,ptrs[i]);
//				if(num_threads==3 && file_counter==121){
//					printf("Read %d words\n",words);
//				}
				if(words>0)//keep reading while we still can
					fwrite(buffer,sizeof(word_read),words,writeTo);
			}
			if(words<numWords) break;//reached the end of the unstriped file
		}

		for(i=0;i<j;i++){
			fclose(ptrs[i]);//close all of the unstriped files we were reading
		}
		fclose(writeTo);//close the striped file we were building


		if(col_counter==numCols){//if we're done scanning all the files, we're done
			break;
		}
	}
	return 1;//return where all the files are stored
}

/*
 * Takes the BITMAP_FILE and reformats to unstriped file folder
 */
int toUnstriped(){
	// printf("in tounstriped() function\n" );
	FILE *fp = fopen(bitmap_file, "r");//try to open the original bitmap
	if(fp == NULL){
		fprintf(stderr,"\nCould not open %s for unstriped reformatting\n",bitmap_file);
		return 0;
	} else {
		char c;//the character we're scanning
		numCols = 0;//counts number of columns
		word_read one = 1;//used for bitwise operations (for longs)


		// printf("\tin tounstriped() else block executed\n" );

		// TODO : fix this while loop
			// Professor Chiu,
			// I've found the problematic line of code, it's found in BitmapEngine/src/Writer.c
			// If you take look at the github repository, its around approximately line 138 of this Writer.c file.
			// https://github.com/aingerson/Bitmap-Engine/blob/master/BitmapEngine/src/Writer.c
			// Here's the line of code :
			// while((c=getc(fp))!=',');//skip the row number and get to the actual data
			// It seems to be an infinite while loop.
		// while((c=getc(fp))!=','){printf("whaa");};//skip the row number and get to the actual data
		// printf("\tin tounstriped() after first while\n" );
		while((c=getc(fp))=='1' || c=='0'){
			numCols++;//just go through the first row to see how many columns this file has
		}
		fseek(fp,0,SEEK_SET);//go back to the beginning (for actually reading the data now)

		char *col_folder=unstripedExt(bitmap_file);

		strcat(col_folder,"/");

		char col_path[BUFF_SIZE];
		snprintf(col_path,BUFF_SIZE,"%s%s",col_folder,"col_\0");//this will eventually be the extension for each column file

		mkdir(col_folder,S_IRWXU);//make the directory to hold all of the files
		float n = (float)(numCols);
		n /= (float)(max);
		iterations = ceil(n);//number of times to iterate //TODO WHY DOES SHE HAVE THIS ITERATION COUNTER

		//printf("Num cols: %d\tIter: %d\n",numCols,iterations);
		int i;
		for(i=0;i<iterations;i++){
			//printf("Iteration %d\n",i);
			fseek(fp,0,SEEK_SET);//go back to the beginning
			int thisNumCols;//the number of columns we're reading in this iteration
			if(i==iterations-1){
				thisNumCols = numCols % max;
				if(thisNumCols==0) thisNumCols=max;
			}
			else{
				thisNumCols = max;
			}
			//printf("Scanning %d columns\n",thisNumCols);

			int j;
			for(j=0;j<thisNumCols;j++){//for every column we're building
				int col = (i*max)+j;//this is the column number
				//printf("\nCol %d\n",col);
				if(col<numCols){//if this column exists
					char buff[BUFF_SIZE];
					snprintf(buff,BUFF_SIZE,"%scol_%d.dat",col_folder,col);//build the name of the column file for each column
					col_files[j] = fopen(buff,"wb");//and open each file for writing
					if(col_files[j]==NULL){
						printf("COULD NOT OPEN FILE\t%s\n",buff);
						break;
					}
					curr[j]=0;//start each first word empty
				}
				else{
					break;
				}
			}
			int r=0;//row counter
			word_count=0;
			while(1){
				if(readRow(fp,&r,i)==0) break;//keep reading rows until it comes back unsuccessful
			}

			if(r!=0){//need to add padding (returned while in the middle of a word)
				int k;
				while(r!=(WORD_READ_LENGTH-1)){
					for(k=0;k<thisNumCols;k++){
						curr[k] <<= one;//shift over 1 (adds one 0 of padding)
					}
					r++;
				}

				for(k=0;k<thisNumCols;k++){//write each word to each column
					fwrite(&(curr[k]),sizeof(word_read),1,col_files[k]);
				}
				word_count++;//we just wrote a word so count it
			}
			int k;
			for(k=0;k<thisNumCols;fclose(col_files[k++]));//close each file
		}
		fclose(fp);//close the bitmap file pointers
		free(col_folder);
		return 1;//return where all the striped files are saved
	}
		return 0;
	}

/*
 * Returns one row from the file pointer (using the iteration and row counter offset)
 */
int readRow(FILE *fp,int *r,int iter){
	char c;//character we're scanning
//	while((c=getc(fp))!=','){//skip the row number
//		if(c==EOF) return 0;//reached the end of the file so return that we were unsuccessful
//	}
	int write = 0;//whether it's time to write or not
	int thisNumCols;//the number of columns in this iteration
	if(iter==iterations-1){
		thisNumCols = numCols % max;
		if(thisNumCols==0) thisNumCols=max;
	}
	else{
		thisNumCols = max;
	}

	if(*r != 0 && (*r) % (WORD_READ_LENGTH-2)==0){//if we're saving the last bit of the word
		*r = -1;//reset the row counter to 0
		write=1;//remember to write
		word_count++;//we know we just finished a whole word
	}

//	int k;
//	for(k=0;k<iter*max;k++){
//		getc(fp);//skip characters depending on which iteration we're on
//	}
	fseek(fp,iter*max,SEEK_CUR);

	int j = 0;//track columns
	word_read one = 1;//used for bitwise operations with longs

	for(;j<thisNumCols;j++){//read every character in this row
		c = getc(fp);//get the next character
		curr[j] <<= one;//shift over one bit

		if(c=='1'){//if it was a one, force a one, otherwise it will be 0 by default (the shift)
			curr[j] |= one;
		}

		if(write){//if it's time to write this word (we've filled up the word for each column)
			//printf("curr[%d] =%u\n", j,curr[j] );
			fwrite(&(curr[j]),sizeof(word_read),1,col_files[j]);//write it to its file
			curr[j]=0;//reset the current word
		}
	}
	(*r)++;//increment the row counter

	while(c=='0' || c=='1'){//skip any new line character
		c = getc(fp);
	}
	if(c==EOF) return 0;//we know there are no more rows to be read

	return 1;//if we got here, we successfully read the row
}

char *unstripedExt(char *file){
	char *path = (char *) malloc(sizeof(char)*BUFF_SIZE);
	snprintf(path,BUFF_SIZE,"%s_UNSTRIPED",file);
	return path;
}

char *stripedExt(char *file, int n){
	char *path = (char *) malloc(sizeof(char)*BUFF_SIZE);
	snprintf(path,BUFF_SIZE,"%s_STRIPED_%d",file,n);
	return path;
}

///**
// * Returns path extension for correctly formatted files
// */
//char *getFormattedExtension(char *bitmap_file){
//	char *extension;
//	if(COMPRESSION==WAH){
//		if(WORD_LENGTH==32){
//			if(FORMAT_TYPE==UNSTRIPED) extension = "UNSTRIPED_WAH32_";//this is the file extension for the column data files
//			else extension = "STRIPED_WAH32_";
//		}
//		else{
//			if(FORMAT_TYPE==UNSTRIPED) extension = "UNSTRIPED_WAH64_";//this is the file extension for the column data files
//			else extension = "STRIPED_WAH64_";
//		}
//	}
//	else if(COMPRESSION==VAL){
//		if(WORD_LENGTH==32){
//			if(FORMAT_TYPE==UNSTRIPED) extension = "UNSTRIPED_VAL32_";//this is the file extension for the column data files
//			else extension = "STRIPED_VAL32_";
//		}
//		else{
//			if(FORMAT_TYPE==UNSTRIPED) extension = "UNSTRIPED_VAL64_";//this is the file extension for the column data files
//			else extension = "STRIPED_VAL64_";
//		}
//	}
//	return getDir(bitmap_file,extension);
//}
