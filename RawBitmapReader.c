#include <stdio.h>

#include "Control.h"
#include "Vars.h"
#include "RawBitmapReader.h"
#include "SegUtil.h"
#include "BlockSeg.h"
#include "WAHCompressor.h"
#include "VALCompressor.h"
#include "Clock.h"
#include "Writer.h"
#include "Core.h"

pthread_t *threads;//thread pointers
char compressed_path[BUFF_SIZE];//the file location for the compressed files
char col_path[BUFF_SIZE];//the path for the temporary column data files
char *uncompressed_path;
int next;//next column ready to be compressed (only used for COL_FORMAT)
pthread_mutex_t mut;//mutex for locking threads
int numCols;//number of columns
int extra;//number of files in last striped file (only used for STRIPED)
struct params **toCompress;//only used for OLD STRIPED (weird threading) format -->probably can delete
int size;//number of words in each column
int numFiles;//number of files (only used for STRIPED)
int currFileNum;//current file number we're scanning (only used for STRIPED)
int blockWords;//number of words can scan per block (per thread for STRIPED)
int colNum;//current column number file we're scanning (only used for STRIPED)
int moreFiles;//whether we're done readifng all the files (only used for STRIPED)
blockSeg **segs;//structs to hold data to pass to compressor
int id[MAX_NUM_THREADS];//threads ids
//word_32 *readingBuffer;//buffer to read data into from columns (only used for STRIPED?)
//word_32 **buffer;

FILE **writingTo;
word_32 *currWord;

int file_offset;

int num_threads;
int format;

struct params {//struct for thread parameters
	word_32 *words;
	int num;
};

FILE *currFile;
blockSeg **nextCol;//next segment each thread is set to compress (start of each stack)
blockSeg **lastCol;//the segment on the top of the stack


/**
 * Initializing all memory/structures needed at start
 */
int initCompression(int striped){
	threads = (pthread_t *) malloc(sizeof(pthread_t) * num_threads);//allocate each thread pointer
	pthread_mutex_init(&mut,NULL);//

	if(striped==UNSTRIPED){
		segs = (blockSeg **) malloc(sizeof(blockSeg *) *num_threads);//pointer for each thread to segment that it's compressing
	}
	else if(striped==STRIPED){
		nextCol = (blockSeg **) malloc(sizeof(blockSeg **) * num_threads);//pointers for the front of the queue
		lastCol = (blockSeg **) malloc(sizeof(blockSeg **) * num_threads);//pointers for the end for the queue
		currWord = (word_32 *) malloc(sizeof(word_32) * num_threads);
		writingTo = (FILE **) malloc(sizeof(FILE *) * num_threads);
	}

	int i;
	for(i=0;i<num_threads;i++){
		id[i]=i;
		if(striped==STRIPED){
			nextCol[i] = (blockSeg *) malloc(sizeof(blockSeg));
			nextCol[i]->toCompress = malloc(sizeof(word_read)*2);
			nextCol[i]->next=NULL;
			nextCol[i]->status = NOT_VALID;
			lastCol[i]=nextCol[i];
		}
	}
	return 1;

}

void clearMem(int striped){
	int i;
	free(threads);
	if(striped==UNSTRIPED){
		for(i=0;i>num_threads;i++){
			free(segs[i]->toCompress);
			free(segs[i]);
		}
		free(segs);
	}
	if(striped==STRIPED){
		for(i=0;i<num_threads;i++){
			free(nextCol[i]->toCompress);
			free(nextCol[i]);
		}

		free(nextCol);
		//free(lastCol);
		free(writingTo);
		free(currWord);
	}
}

//original bitmap file, STRIPED/UNSTRIPED, WAH/VAL, num_threads
double compress(char *file, int striped, int form, int n){
	num_threads = n;
	format = form;

	initCompression(striped);

	if(striped==UNSTRIPED){
		//uncompressed_path=unstripedExt(file); //TODO I changed this in Alexia's code. Unsure if correct yet
		uncompressed_path = file;
		snprintf(compressed_path,BUFF_SIZE,"%s_%d_COMPRESSED/",uncompressed_path,num_threads);
	}
	else{
		uncompressed_path=stripedExt(file,num_threads);
		snprintf(compressed_path,BUFF_SIZE,"%s_COMPRESSED/",uncompressed_path);
	}

	mkdir(compressed_path,S_IRWXU);

	strcat(uncompressed_path, "/col_");
	strcat(compressed_path,"col_");

	numCols = 0;
	size = 0;
	next = 0;

	runOverhead(striped);


	double start = rtclock();
	if(striped==STRIPED) compressStriped();
	else compressUnstriped();

	double end = rtclock();
	return end-start;
}

/*
 * Runs basic overhead needed to prepare the program for compression
 */
void runOverhead(int striped){
	if(striped==UNSTRIPED){
		numCols = 0;
		while(1){
			char temp_name[BUFF_SIZE];
			snprintf(temp_name,BUFF_SIZE,"%s%d.dat",uncompressed_path,numCols);
			if(size==0){
				struct stat st;
				stat(temp_name, &st);
				size = (st.st_size)/sizeof(word_read);//this is number of words in each column file (same in each column)
			}
			//counting the number of columns there are in that folder
			if(access(temp_name,F_OK) != -1){//if this file exists
				numCols++;//count it
				continue;//and keep counting
			}
			else{//this file doesn't exit so we know how many columns exist
				break;
			}
		}

		if(CORE==OUT_CORE){
			blockWords = (BLOCK_SIZE*1000)/sizeof(word_read);//this is how many words we will be scanning every time

			//TODO: implement VAL here too (focusing on WAH right now)
			//if(COMPRESSION==VAL){
			//	blockWords -= (blockWords % sizeof(word_32));
			//}
			//blockWords -= (blockWords%NUM_THREADS);//(must be divisible by the number of number of threads)
		}
		else{
			blockWords = size;
		}

	}
	else if(striped==STRIPED){
		extra = num_threads-1;//the number of left over columns saved in the last striped file
		numCols=-1;
		while(1){//counting up
			numCols+=num_threads;

			char temp_name[BUFF_SIZE];
			snprintf(temp_name,BUFF_SIZE,"%s%d.dat",uncompressed_path,numCols);
			if(access(temp_name,F_OK) != -1){//if this file exists
				if(size==0){
					struct stat st;
					stat(temp_name, &st);
					size = (st.st_size)/sizeof(word_read);
					size /= num_threads;//the number of words in each column
				}
				continue;//keep counting more files
			}
			else{//this file doesn't exist so we're reaching the end
				break;
			}

		}
		while(1){//counting down

			numCols--;
			//printf("DOWN Num Cols: %d\n",numCols);

			char temp_name[BUFF_SIZE];
			snprintf(temp_name,BUFF_SIZE,"%s%d.dat",uncompressed_path,numCols);
			if(access(temp_name,F_OK) != -1){//if this file exists, we've found the last file
				break;
			}
			else{//need to keep going to know how many columns there are
				extra--;//tracks the number of columns saved in the last files
				continue;
			}
		}
		numCols++;//numCols was the last index so increment to represent count
		if(extra==0) extra = num_threads;
		float n = (float)(numCols);
		n/=(float)(num_threads);
		numFiles = ceil(n);//figure out the number of striped files we will be scanning
		currFileNum=0;//start the file counter at the beginning
		blockWords = BLOCK_SIZE*1000/sizeof(word_read);//this is how many words we will be scanning every time
		int perCol = blockWords/num_threads;
		blockWords = perCol*num_threads;
		colNum=num_threads-1;
		moreFiles=1;


	}
	initUtilSegs(format);

}



int readNextBlock(FILE *toRead, int off, int colsPerFile, blockSeg **saving){
	//printf("Num cols: %d\n",colsPerFile);
	fseek(toRead,off*sizeof(word_read),SEEK_SET);
	int block = (BLOCK_SIZE*1000)/sizeof(word_read);
	block -= block%colsPerFile;

	if((block+off)>size*colsPerFile){//last column, adjust block
		block=(size*colsPerFile)-off;
	}

	int i;
	for(i=0;i<colsPerFile;i++){

		saving[i]->toCompress = (word_read *) malloc(sizeof(word_read)*(block/colsPerFile));
		saving[i]->size=fread(saving[i]->toCompress,sizeof(word_read),block/colsPerFile,toRead);

		if(off==0){
			if(off+block>=(size*colsPerFile)){
				saving[i]->status=FIRST_LAST;
			}
			else{
				saving[i]->status=READ_FIRST;
			}
		}
		else if(off+block>=(size*colsPerFile)){
			saving[i]->status=LAST_BLOCK;
		}
		else{
			saving[i]->status=VALID;
		}
	}
	for(;i<num_threads;i++){//don't need the other segments, mark NOT_VALID
		saving[i]->status=NOT_VALID;
		saving[i]->toCompress = (word_read *) malloc(sizeof(word_read)*2);//for consistency in freeing mem
	}

	fclose(toRead);
	return 1;
}

/*
 * Starts compression of the striped files
 */
void compressStriped(){
	int i;
	for(i=0;i<num_threads;i++){//start each thread going
		if(pthread_create(&threads[i],NULL,startThreadStriped,(void *)(&(id[i])))){
			printf("Error creating thread\n");
			return;
		}
	}

	for(i=0;i<num_threads;i++){//wait for all the threads to finish
		if(pthread_join(threads[i],NULL)){
			printf("Error joining thread\n");
			return;
		}
	}
	//printQ();
}


/**
 * Compresses the next column that has not been compressed yet
 */
void *startThreadStriped(void *param){
	int *id = (int *) param;
	while(1){
		pthread_mutex_lock(&mut);
		if(nextCol[*id]->status==NOT_VALID || nextCol[*id]->status==READING){

			if(moreFiles){
				blockSeg **tempData = (blockSeg **) malloc(sizeof(blockSeg *) * num_threads);
				int i;
				for(i=0;i<num_threads;i++){
					lastCol[i]->next = (blockSeg *) malloc(sizeof(blockSeg));
					tempData[i] = lastCol[i]->next;
					tempData[i]->status = READING;
					lastCol[i]=lastCol[i]->next;
					lastCol[i]->next=NULL;
					tempData[i]->colNum=(currFileNum*num_threads)+i;
				}

				char name[BUFF_SIZE];
				snprintf(name,BUFF_SIZE,"%s%d.dat",uncompressed_path,colNum);

				FILE *nextFile = fopen(name,"rb");
				int off = file_offset;

				int colsToRead = num_threads;
				if(currFileNum==numFiles-1) colsToRead = extra;
				blockWords = BLOCK_SIZE*1000/sizeof(word_read);
				blockWords-=blockWords%colsToRead;

				file_offset+=blockWords;
				if(file_offset>=(size*colsToRead)){
					currFileNum++;
					if(currFileNum==numFiles-1){
						colNum+=extra;
					}
					else if(currFileNum<numFiles-1){
						colNum+=num_threads;
					}
					else{
						moreFiles = 0;
					}
					file_offset=0;
				}

				if(PAR_READ) pthread_mutex_unlock(&mut);
				readNextBlock(nextFile,off,colsToRead,tempData);
				if(PAR_READ) pthread_mutex_lock(&mut);

				if(nextCol[*id]->status==NOT_VALID){

					blockSeg *temp = nextCol[*id]->next;
					free(nextCol[*id]->toCompress);
					free(nextCol[*id]);
					nextCol[*id]=temp;
				}
			}
			if(nextCol[*id]->status==NOT_VALID && (nextCol[*id]->next==NULL || nextCol[*id]->next->status==NOT_VALID) && !moreFiles){
				pthread_mutex_unlock(&mut);
				return NULL;
			}

			pthread_mutex_unlock(&mut);
		}
		else{//ready to compress
			pthread_mutex_unlock(&mut);
			if(nextCol[*id]->status==READ_FIRST || nextCol[*id]->status==FIRST_LAST){
				char name[BUFF_SIZE];
				snprintf(name,BUFF_SIZE,"%s%d.dat",compressed_path,nextCol[*id]->colNum);
				writingTo[*id]=fopen(name,"wb");
				if(writingTo[*id]==NULL) printf("Could not open file\n");
			}
			nextCol[*id]->colFile=writingTo[*id];
			nextCol[*id]->curr=currWord[*id];

			if(format==WAH) compressUsingWAH(nextCol[*id]);

			if(nextCol[*id]->status==LAST_BLOCK || nextCol[*id]->status==FIRST_LAST){
				fclose(writingTo[*id]);
			}
			else{
				currWord[*id]=nextCol[*id]->curr;
			}
			nextCol[*id]->status=NOT_VALID;

			pthread_mutex_lock(&mut);
			if(nextCol[*id]->next!=NULL){
				blockSeg *temp = nextCol[*id]->next;
				free(nextCol[*id]->toCompress);
				free(nextCol[*id]);
				nextCol[*id]=temp;
			}
			pthread_mutex_unlock(&mut);
		}
	}
	return NULL;
}



void printQ(){
	printf("PRINTING\n");
	int k;
	for(k=0;k<num_threads;k++){
		printf("NEXT COL [%d]\n",k);
		if(nextCol[k]==NULL) continue;
		blockSeg *nxt = nextCol[k];
		int count=0;
		while(nxt!=NULL && count++<4){
			printf("\tStatus: %d\tCol: %d\n",nxt->status,nxt->colNum);
			nxt=nxt->next;
		}

		printf("LAST COL [%d]\n",k);
		if(lastCol[k]!=NULL) printf("\tStatus: %d\tCol: %d\n",lastCol[k]->status,lastCol[k]->colNum);
		else printf("\tLAST COL NULL\n");

	}
}

//UNSTRIPED METHODS


/*
 * Starts compressing the bitmap data after saved in separate column files
 */
void compressUnstriped(){
	//printf("Compress Unstriped\n");

	int i;
	for(i=0;i<num_threads;i++){//start each thread going
		segs[i] = (blockSeg *) malloc(sizeof(blockSeg));//allocate the segment pointer
		segs[i]->toCompress = (word_read *) malloc(sizeof(word_read) * blockWords);//allocate the word array buffer in the segment struct
		if(pthread_create(&threads[i],NULL,compressNext,(void *)(&(id[i])))){
			printf("Error creating thread\n");
			return;
		}
	}

	for(i=0;i<num_threads;i++){//wait for all the threads to finish
		if(pthread_join(threads[i],NULL)){
			printf("Error joining thread\n");
			return;
		}
	}
}

/**
 * Compresses the next column that has not been compressed yet
 */
void *compressNext(void *param){
	int n = -1;//hasn't been assigned a column yet
	int *id = (int *) param;
	while(n<numCols){
		pthread_mutex_lock(&mut);//lock everything
		n=(next++);//find out which column we need to compress (and increment for the next thread to compress)
		pthread_mutex_unlock(&mut);//unlock it

		if(n<numCols){//if there's still another column to compress
			compressColumn(n,*id);//go for it
		}
	}
	return NULL;
}



/**
 * Compresses one column (colNum = col)
 */
void compressColumn(int col, int threadNum){
	int runs = 1;
	if(NUM_SEGS==-1) runs=3;
	int i;
	int length = BASE_LEN;
	if(NUM_SEGS>-1) length = (WORD_LENGTH-FLAG_BITS)/NUM_SEGS;

	unsigned int min = 0;
	min--;
	int optimal=0;
	for(i=0;i<runs;i++){

		char reading[BUFF_SIZE];
		snprintf(reading,BUFF_SIZE,"%s%d.dat",uncompressed_path,col);
		FILE *ptr = fopen(reading,"rb");//open the uncompressed file
		int read=blockWords;//will hold how many words have been successfully read

		segs[threadNum]->colNum = col;//save colNumber of this block
		if(CORE==OUT_CORE) segs[threadNum]->status=READ_FIRST;//we know it's the first one
		else segs[threadNum]->status = FIRST_LAST;

		char writing[BUFF_SIZE];
		snprintf(writing,BUFF_SIZE,"%s%d.dat",compressed_path,col);

		segs[threadNum]->colFile = fopen(writing,"wb");//open the respective file for writing
		int numWords = 0;

		while(read==blockWords){//while we still can read
			read = fread(segs[threadNum]->toCompress, sizeof(word_read), blockWords, ptr);//transfer over all the words
			if(read<blockWords){
				if(segs[threadNum]->status==READ_FIRST){
					 segs[threadNum]->status=FIRST_LAST;
				 }
				else{
					segs[threadNum]->status = LAST_BLOCK;
				}
			}
			segs[threadNum]->size = read;//and save how many words there are in there

			if(format==WAH){
				 compressUsingWAH(segs[threadNum]);//compress it
			 }
			else if(format==VAL) {
				numWords += compressUsingVAL(segs[threadNum],length);
			}

			if(CORE==IN_CORE) break;
			segs[threadNum]->status = VALID;//not at the beginning anymore
			if(read<blockWords){//if we reached the end of the file
				fclose(segs[threadNum]->colFile);//close the compressed file
				fclose(ptr);//close the uncompressed column data file
				break;//and leave
			}
		}

		if(format==VAL && NUM_SEGS==-1){
			if(numWords<min){//if the column we just compressed is smaller than a previous max
				optimal = length;
				min=numWords;//save it as the new minimum size
				char rmv[BUFF_SIZE];
				snprintf(rmv,BUFF_SIZE,"%s%d.dat",compressed_path,numCols+threadNum);
				char rnm[BUFF_SIZE];
				snprintf(rnm,BUFF_SIZE,"%s%d.dat",compressed_path,col);
				char last[BUFF_SIZE];
				snprintf(last,BUFF_SIZE,"%s%d.dat",compressed_path,numCols);

				remove(rmv);//delete whatever we were saving there before
				rename(rnm,last);//save min column there for later
			}
			length+=BASE_LEN;
		}
	}
	if(format==VAL && NUM_SEGS==-1){
		char rmv[BUFF_SIZE];
		snprintf(rmv,BUFF_SIZE,"%s%d.dat",compressed_path,numCols+threadNum);
		char rnm[BUFF_SIZE];
		snprintf(rnm,BUFF_SIZE,"%s%d.dat",compressed_path,col);

		rename(rmv,rnm);//save the smallest column to be where we want it
	}
}
