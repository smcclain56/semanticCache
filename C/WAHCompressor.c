#include <stdlib.h>
#include <stdio.h>
#include "WAHCompressor.h"
#include "Control.h"
#include "SegUtil.h"

/*
 * Compresses one segment of a column
 */
void compressUsingWAH(blockSeg *param){
	int numWords = param->size;//number of words in this block
	int j;//the word we're scanning
	int prev;//this tracks what the word before it was (the type)

	if(param->status==READ_FIRST || param->status==FIRST_LAST){//if this block is the first of the column
		param->curr = param->toCompress[0];
		j=1;//we're starting to read the 2nd word (first one is automatically the current word)
		word_32 len = WORD_LENGTH-1;
		fwrite(&len, sizeof(word_32),1,param->colFile);//write the word length to file
	}
	else{
		j=0;
	}

	prev = getType(param->curr,WORD_LENGTH);//find the type of the previous compressed word (to see if we can concatenate)

	for(;j<numWords;j++){//go through every uncompressed word
		int next = getType(param->toCompress[j],WORD_LENGTH);//the type of this word

		//the next word is a run of 0s
		if(next==ZERO_LIT){//this one and the one before it were both literal runs of zeros so put it in a new fill word of 0s
			if(prev==ZERO_LIT){
				param->curr = getZeroFill(WORD_LENGTH);
				prev = ZERO_RUN;
			}
			else if(prev==ZERO_RUN){//this one is a run of zeros and the one before it was already a fill of 0s
				//but the previous one is full (can't increment anymore) so we still need to make this a literal (keep as is)
				if(param->curr==getZeroFill(WORD_LENGTH)){
					fwrite(&(param->curr), sizeof(word_32),1,param->colFile);
					param->curr = param->toCompress[j];
					prev = next;
				}
				else{//this means that we can increment the zero run in the previous spot
					param->curr += 1;
				}
			}
			else{//the current one is a zero literal but the word before has both 0s and 1s (we can't do anything), so just save as is and keep going
				fwrite(&(param->curr), sizeof(word_32),1,param->colFile);
				param->curr = param->toCompress[j];
				prev = next;
			}
		}
		else if(next==ONE_LIT){
			if(prev==ONE_LIT){//if the last one was a literal of ones and this one was too, put them together
				param->curr = getOneFill(WORD_LENGTH);
				prev = ONE_RUN;
			}
			else if(prev==ONE_RUN){//we want to increment the last one but we can't because it's full so
									//so we still have to keep the literal
				if(param->curr==getMaxOneFill(WORD_LENGTH)){
					fwrite(&(param->curr), sizeof(word_32),1,param->colFile);
					param->curr = param->toCompress[j];
					prev = next;
				}
				else{//the one before this was a fill of ones, so just increment the last fill
					param->curr += 1;
				}
			}
			else{//when the current one is a literal of all ones
				//if the previous one is not a run or literal of ones
				 //(either literal or run of zeros), just save as the literal that it is
				fwrite(&(param->curr), sizeof(word_32),1,param->colFile);
				param->curr = param->toCompress[j];
				prev = next;
			}
		}
		else{//the word is neither a run of 0s or a run of 1s so it's a literal --> just save and continue
			fwrite(&(param->curr), sizeof(word_32),1,param->colFile);
			param->curr = param->toCompress[j];
			prev = next;
		}
	}
	if(param->status==LAST_BLOCK || param->status==FIRST_LAST){//if this is the last block of the column
		fwrite(&(param->curr), sizeof(word_32),1,param->colFile);//write the last word
	}
}
