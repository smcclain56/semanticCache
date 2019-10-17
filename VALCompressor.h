#ifndef VALCOMPRESSOR_H_
#define VALCOMPRESSOR_H_

#include "RawBitmapReader.h"
#include "BlockSeg.h"
#include "Control.h"

unsigned int compressUsingVAL(blockSeg *,int);
void initVALCompressorSegs();
word_32 getNextSegment(word_read *,int, int);
word_32 addToStored(word_32,word_32,int);

typedef unsigned int word_write;

#endif /* VALCOMPRESSOR_H_ */
