#ifndef SEGUTIL_H_
#define SEGUTIL_H_

#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include "Control.h"

#define LITERAL -1//a literal word (0...0111)
#define ZERO_LIT 0//a literal of all zeros (000000...00)
#define ONE_LIT 1//a literal of all ones(01111111..11)
#define ZERO_RUN 2//a fill of zeros (100000...10)
#define ONE_RUN 3//a fill of ones (1100000...10)

//different blockSeg statuses:
#define NOT_VALID -1//this block is empty (no information, cannot be compressed)
#define EMPTY_FIRST 0//no information in segment but marks the beginning of a column
#define READ_FIRST 1//valid block with data (needs to be compressed)
#define VALID 2//block in the middle of a column
#define LAST_BLOCK 3//block is the last block of a column (needs to be written)
#define FIRST_LAST 4//block is both the first and last block of the column
#define READING 5

word_32 getZeroFill(word_32);
word_32 createZeroFill(word_32);

word_32 getOneFill(word_32);
word_32 createOneFill(word_32);

word_32 getMaxZeroFill(word_32);
word_32 createMaxZeroFill(word_32);

word_32 getMaxOneFill(word_32);
word_32 createMaxOneFill(word_32);

word_32 getOneLit(word_32);
word_32 createOneLit(word_32);


//other utilities
int getType(word_32,word_32);
word_32 getNumRuns(word_32,word_32);
word_32 getFillType(word_32,word_32);

void initUtilSegs(int);
#endif /* SEGUTIL_H_ */


