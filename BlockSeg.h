#ifndef BLOCKSEG_H_
#define BLOCKSEG_H_

#include "Control.h"

//struct that holds uncompressed data to send to compressor
typedef struct blockSeg{
	word_read *toCompress;//the current block seg of words
	int size;//number of words in toCompress
	int colNum;//column num compressed
	struct blockSeg *next;//next one to be compressed
	FILE *colFile;//where all the compressed words are going
	word_32 curr;//the latest compressed
	word_32 stored;
	int status;//first/last block of column? or not valid block (empty)?
} blockSeg;

#endif /* BLOCKSEG_H_ */
