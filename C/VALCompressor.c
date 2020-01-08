
#include <stdlib.h>
#include <stdio.h>
#include "VALCompressor.h"
#include "Control.h"
#include "SegUtil.h"

/**
 * Compresses the block of data stored in param using VAL, according to the segment length specified
 * Writes compressed data to file pointer specified in blockSeg struct -> colFile
 */
unsigned int compressUsingVAL(blockSeg *param, int segLength){
	unsigned totalWords = 0;
	int jump = segLength/BASE_LEN;//number of read words to jump by for every compression word
	int segsPerWord = (int) WORD_LENGTH/segLength;
	int segCount = 0;

	int numWords = param->size*sizeof(word_read);//number of words in this block
		int j=0;//the word we're scanning

		if(param->status==READ_FIRST || param->status==FIRST_LAST){//if this block is the first of the column
			param->curr = getNextSegment(param->toCompress,j,jump);
			j+=jump;//we're starting to read the 2nd word (first one is automatically the current word)
			word_32 len = segLength;
			fwrite(&len, sizeof(word_32),1,param->colFile);//write the word length to file
			param->stored = 0;
		}

		word_32 nextSeg;//next segment scanned
		int next;//type of the next word
		int prev = getType(param->curr,segLength+1);//find the type of the previous compressed word (to see if we can concatenate)

		for(;j<numWords;){//go through every uncompressed word
			nextSeg = getNextSegment(param->toCompress,j,jump);
			next = getType(nextSeg,segLength+1);//the type of this word
			j+=jump;
			//the next word is a run of 0s
			if(next==ZERO_LIT){
				if(prev==ZERO_LIT){//this one and the one before it were both literal runs of zeros so put it in a new fill word of 0s
					param->curr = getZeroFill(segLength);
					prev = ZERO_RUN;
				}
				else if(prev==ZERO_RUN){//this one is a run of zeros and the one before it was already a fill of 0s
					//but the previous one is full (can't increment anymore) so we still need to make this a literal (keep as is)
					if(param->curr==getMaxZeroFill(segLength)){
						param->stored = addToStored(param->stored,param->curr,segLength);
						segCount++;
						param->curr = nextSeg;
						prev = next;
					}
					else{//this means that we can increment the zero run in the previous spot
						param->curr += 1;
					}
				}
				else{//the current one is a zero literal but the word before has both 0s and 1s (we can't do anything), so just save as is and keep going
					param->stored = addToStored(param->stored,param->curr,segLength);
					segCount++;
					param->curr = nextSeg;
					prev = next;
				}
			}
			else if(next==ONE_LIT){
				if(prev==ONE_LIT){//if the last one was a literal of ones and this one was too, put them together
					param->curr = getOneFill(segLength);
					prev = ONE_RUN;
				}
				else if(prev==ONE_RUN){//we want to increment the last one but we can't because it's full so
										//so we still have to keep the literal
					if(param->curr==getMaxOneFill(segLength)){
						param->stored = addToStored(param->stored,param->curr,segLength);
						segCount++;
						param->curr = nextSeg;
						prev = next;
					}
					else{//the one before this was a fill of ones, so just increment the last fill
						param->curr += 1;
					}
				}
				else{//when the current one is a literal of all ones
					//if the previous one is not a run or literal of ones
					 //(either literal or run of zeros), just save as the literal that it is
					param->stored = addToStored(param->stored,param->curr,segLength);
					segCount++;
					param->curr = nextSeg;
					prev = next;
				}
			}
			else{//the word is neither a run of 0s or a run of 1s so it's a literal --> just save and continue
				param->stored = addToStored(param->stored,param->curr,segLength);
				segCount++;
				param->curr = nextSeg;
				prev = next;
			}

			if(segCount==segsPerWord){//maxed out the segments in the stored word
				fwrite(&(param->stored),sizeof(word_32),1,param->colFile);
				totalWords++;
				segCount=0;
				param->stored=0;
			}

		}
		if(param->status==LAST_BLOCK || param->status==FIRST_LAST){//if this is the last block of the column
			param->stored = addToStored(param->stored,param->curr,segLength);
			segCount++;
			if(segCount>0){
				while(segCount<segsPerWord){//add padding
					param->stored = addToStored(param->stored,0,segLength);
					segCount++;
				}
				fwrite(&(param->stored), sizeof(word_32),1,param->colFile);//write the last word
				totalWords++;
			}
		}

		return totalWords;
}

/**
 * Returns one segment composed of jump number of segments starting at fromFile[index]
 */
word_32 getNextSegment(word_read *fromFile, int index, int jump){
	word_32 ret = 0;
	int i;
	for(i=0;i<jump;i++){
		word_32 next = (word_32) fromFile[index+i];
		ret <<= (WORD_READ_LENGTH-1);
		ret |= next;
	}
	return ret;
}

/**
 * Adds segment to word currently being built (by shifting over data and adding it to the least significant bits
 */
word_32 addToStored(word_32 previous, word_32 next, int segLength){
	word_32 prevFlags = previous>>((sizeof(word_32)*8)-(FLAG_BITS+1));
	word_32 nextFlag = next>>segLength;
	word_32 newFlags = (prevFlags|nextFlag) << (sizeof(word_32)*8-FLAG_BITS);

	previous <<= segLength;//shift previous over to make room for new segment
	word_32 shiftBits = WORD_LENGTH-segLength;//number of extra/insignificant bits in the new word
	previous |= ((next<<shiftBits)>>shiftBits);//make sure everything extra is a 0 in the new segment and add it to the previous one
	previous |= newFlags;//add the flags back on

	return previous;
}
