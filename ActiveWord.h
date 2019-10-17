
#ifndef ACTIVEWORD_H_
#define ACTIVEWORD_H_

#include "Control.h"

typedef struct activeWord{
	word_32 *flag;//list of segment flags (0-literal, 1-run)
	word_32 *seg;//list of segment data
	int numSegs;//total number of segments (flag[numSegs],seg[numSegs])
	int currSeg;//index tracker while cycling through segments
	int length;//length of each segment in seg[]
	int decoding;//boolean marker to track if currently decoding a word
} activeWord;

activeWord *initActiveWord(int);
void updateActiveWord(activeWord *,word_32);
word_32 createWord(activeWord *);
void freeActive(activeWord *);

//for testing
void printActive(activeWord *);

#endif /* ACTIVEWORD_H_ */
