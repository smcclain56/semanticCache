#include <stdio.h>
#include <stdlib.h>
#include "QueryUtil.h"
#include "SegUtil.h"

/**
 * ORs the next 2 segments (2 fills) and stores result
 */
void fillORfill(activeWord *smallFill, activeWord *bigFill, activeWord *result){
	word_32 runSize = getNumRuns(smallFill->seg[smallFill->currSeg],smallFill->length);
	bigFill->seg[bigFill->currSeg] -= runSize;
	if(getFillType(smallFill->seg[smallFill->currSeg],smallFill->length)==1 || getFillType(bigFill->seg[bigFill->currSeg],bigFill->length)==1){
		runSize |= 1<<(smallFill->length-1);
	}
	if(getNumRuns(bigFill->seg[bigFill->currSeg],bigFill->length)==0) bigFill->currSeg++;
	result->flag[0]=1;
	result->seg[0]=runSize;
	smallFill->currSeg++;
}

/**
 * ORs the next 2 segments (2 lits) and stores result
 */
void litORlit(activeWord *lit1, activeWord *lit2, activeWord *result){
	result->seg[0] = lit1->seg[lit1->currSeg] | lit2->seg[lit2->currSeg];
	lit1->currSeg++;
	lit2->currSeg++;
	if(result->seg[0]==0){//all 0s - turn into one run of 0s (for consistency)
		result->flag[0]=1;
		result->seg[0]++;
	}
	else if(result->seg[0]==getOneLit(result->length+1)){//all 1s - turn into one run of 1s (for consistency)
		result->flag[0]=1;
		result->seg[0]=getOneFill(result->length+1)-1;
	}
	else{
		result->flag[0]=0;
	}
}

/**
 * ORs the next 2 segments (1 fill, 1 lit) and stores result
 */
void fillORlit(activeWord *fill, activeWord *lit, activeWord *result){
	if(getFillType(fill->seg[fill->currSeg],fill->length)==1){//run of 1s so result is all 1s
		result->flag[0]=1;
		result->seg[0]=(1<<(fill->length-1)) + 1;
	}
	else{//run of 0s so result is lit
		result->flag[0]=0;
		result->seg[0]=lit->seg[lit->currSeg];
		if(result->seg[0]==0){//all 0s - turn into one run of 0s (for consistency)
			result->flag[0]=1;
			result->seg[0]++;
		}
		else if(result->seg[0]==getOneLit(result->length+1)){//all 1s - turn into one run of 1s (for consistency)
			result->flag[0]=1;
			result->seg[0]=getOneFill(result->length+1)-1;
		}
	}
	lit->currSeg++;
	fill->seg[fill->currSeg]--;
	if(getNumRuns(fill->seg[fill->currSeg],fill->length)==0) fill->currSeg++;

}

/**
 * ANDs the next 2 segments (2 fills) and stores result
 */
void fillANDfill(activeWord *smallFill, activeWord *bigFill, activeWord *result){

	word_32 runSize = getNumRuns(smallFill->seg[smallFill->currSeg],smallFill->length);
	bigFill->seg[bigFill->currSeg] -= runSize;
	if(getFillType(smallFill->seg[smallFill->currSeg],smallFill->length)==1 && getFillType(bigFill->seg[bigFill->currSeg],bigFill->length)==1){//both runs of 1s
		runSize |= 1<<(smallFill->length-1);
	}
	if(getNumRuns(bigFill->seg[bigFill->currSeg],bigFill->length)==0) bigFill->currSeg++;
	result->flag[0]=1;
	result->seg[0]=runSize;
	smallFill->currSeg++;
}

/**
 * ANDs the next 2 segments (2 lits) and stores results
 */
void litANDlit(activeWord *lit1, activeWord *lit2, activeWord *result){
	result->seg[0] = lit1->seg[lit1->currSeg] & lit2->seg[lit2->currSeg];
	lit1->currSeg++;
	lit2->currSeg++;
	if(result->seg[0]==0){//all 0s - turn into one run of 0s (for consistency)
		result->flag[0]=1;
		result->seg[0]++;
	}
	else if(result->seg[0]==getOneLit(result->length+1)){//all 1s - turn into one run of 1s (for consistency)
		result->flag[0]=1;
		result->seg[0]=getOneFill(result->length+1)-1;
	}
	else{
		result->flag[0]=0;
	}
}

/**
 * ANDs the next 2 segments (1 fill, 1 lit) and stores result
 */
void fillANDlit(activeWord *fill, activeWord *lit, activeWord *result){
	if((fill->seg[fill->currSeg])>>(fill->length-1)==1){//run of 1s so result is lit
		result->flag[0]=0;
		result->seg[0]=lit->seg[lit->currSeg];
		if(result->seg[0]==0){//all 0s - turn into one run of 0s (for consistency)
			result->flag[0]=1;
			result->seg[0]++;
		}
		else if(result->seg[0]==getOneLit(result->length+1)){//all 1s - turn into one run of 1s (for consistency)
			result->flag[0]=1;
			result->seg[0]=getOneFill(result->length+1)-1;
		}
	}
	else{//run of 0s so result is 0
		result->flag[0]=1;
		result->seg[0]=1;
	}
	lit->currSeg++;
	fill->seg[fill->currSeg]--;
	if(getNumRuns(fill->seg[fill->currSeg],fill->length)==0) fill->currSeg++;

}

/**
 * Appends toAdd to the end of the result (to previous if possible; if previous is exhausted, saves previous into result[index])
 */
void append(word_32 *result, int *index, activeWord *previous, activeWord *toAdd){
	//special case if there is nothing in previous (first segment of query)
	if(previous->currSeg==-1){
		previous->currSeg=0;
		previous->flag[0]=toAdd->flag[0];
		previous->seg[0]=toAdd->seg[0];
		if(PRINT) printf("First append\n");

		return;
	}

	word_32 typePrev = getFillType(previous->seg[previous->currSeg],previous->length);
	word_32 typeNext = getFillType(toAdd->seg[0],toAdd->length);

	if(previous->flag[previous->currSeg]==1 && toAdd->flag[0]==1 && typePrev==typeNext){//can consolidate
		word_32 sizePrev = getNumRuns(previous->seg[previous->currSeg],previous->length);
		word_32 sizeNext = getNumRuns(toAdd->seg[0],toAdd->length);
		word_32 max = getOneLit(toAdd->length+1);//max runs is same as literal of ones
		if(sizePrev+sizeNext>max){//exceeds limit, update previous to max and transfer next to previous (subtracted)
			previous->seg[previous->currSeg] += (max-sizePrev);//transfer all we can not exceeding limit
			toAdd->seg[0] -= (max-sizePrev);//subtract what we transfered from the next one
		}
		else{//can add it without exhausting the previous
			previous->seg[previous->currSeg] += sizeNext;
			return;
		}
	}

	//cannot consolidate (either one of them is a literal or both are runs but of different types)
	//special case: could consolidate but when we added number of runs, exceed the limit so need to move on
	if(previous->flag[previous->currSeg]==1 && getNumRuns(previous->seg[previous->currSeg],previous->length)==1){//if the previous is only a run of 1
		previous->flag[previous->currSeg]=0;//change it to a literal
		if(typePrev==1){//need to save as literal of 1s
			previous->seg[previous->currSeg]=getOneLit(previous->length+1);
		}
		else{//save a literal of 0s (just 0)
			previous->seg[previous->currSeg]=0;
		}
	}

	previous->currSeg++;//moving on to next segment
	if(previous->currSeg>=previous->numSegs){//need to write first before adding
		word_32 toWrite = createWord(previous);
		result[(*index)++] = toWrite;
		previous->currSeg=0;
	}

	//we've made sure previous is adequate (not run of 1s and made sure we wrote it if we exhausted it)
	//so now we can save the next word as the previous segment
	previous->flag[previous->currSeg] = toAdd->flag[0];
	previous->seg[previous->currSeg] = toAdd->seg[0];
}
