#include "ActiveWord.h"
#include "Control.h"

#include <stdio.h>
#include <stdlib.h>
/**
 * Allocates memory for activeWord that tracks segment data for word of length s
 * Returns pointer to the activeWord struct
 */
activeWord *initActiveWord(int s){
	activeWord *ret = (activeWord *) malloc(sizeof(activeWord));
	ret->length=s;
	ret->numSegs = WORD_LENGTH/s;

	ret->flag = (word_32 *) malloc(sizeof(word_32)*ret->numSegs);
	ret->seg = (word_32 *) malloc(sizeof(word_32)*ret->numSegs);
	ret->currSeg=0;
	ret->decoding=0;
	int i;
	for(i=0;i<ret->numSegs;i++){//set all segs to 0
		ret->flag[i]=0;
		ret->seg[i]=0;
	}
	return ret;
}

/**
 * Frees all memory allocated in the pointer
 */
void freeActive(activeWord *active){
	free(active->flag);
	free(active->seg);
	free(active);
}

/**
 * Updates activeWord to reflect data in newWord
 */
void updateActiveWord(activeWord *toUpdate, word_32 newWord){
	int i;
	for(i=0;i<toUpdate->numSegs;i++){
		toUpdate->flag[i]=(newWord<<(FLAG_BITS-(toUpdate->numSegs)+i))>>((toUpdate->length*toUpdate->numSegs)+(FLAG_BITS-1));
		toUpdate->seg[i]=(newWord<<(FLAG_BITS+(toUpdate->length*i)))>>(WORD_LENGTH-toUpdate->length);
	}
	toUpdate->currSeg=0;
}

/**
 * Creates a word from the data saved in active struct
 */
word_32 createWord(activeWord *active){
	word_32 flags=0;
	word_32 segs=0;

	int i;
	for(i=0;i<active->numSegs;i++){
		flags <<= 1;
		flags |= (active->flag[i]);
		segs <<= active->length;
		segs |= active->seg[i];
	}
	return (flags<<(active->numSegs*active->length)) | segs;
}

/**
 * Prints activeWord (for testing)
 */
void printActive(activeWord *print){
	int i;
	for(i=0;i<print->numSegs;i++){
		printf("Flag: %d\tSeg: %x\n",print->flag[i],print->seg[i]);
	}
}
