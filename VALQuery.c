#include "VALQuery.h"
#include "SegUtil.h"
#include "QueryUtil.h"


#include <stdio.h>
#include "Core.h"

// #include <stdlib.h>
// #include <math.h>

/**
 * Performs AND between col0 and col1 and saves result into ret
 */
int AND_VAL(word_32 *ret, word_32 *col0, int sz0, word_32 *col1, int sz1){
	int c0,c1,s0,s1, index;
	index = 0;//position of next word being written to the result
	c0=c1=1;
	s0=col0[0];
	s1=col1[0];
	word_32 decodeLength=s0;

	if(DECODE==DECODE_UP){
		if(s0<s1) decodeLength=s1;
	}
	else{
		if(s1<s0) decodeLength=s1;
	}
	ret[index++]=decodeLength;

	activeWord *activeRead0 = initActiveWord(s0);
	activeWord *activeRead1 = initActiveWord(s1);
	activeWord *activeAligned0 = initActiveWord(decodeLength);
	activeWord *activeAligned1 = initActiveWord(decodeLength);
	activeWord *resultTemp = initActiveWord(decodeLength);

	activeWord *previous = initActiveWord(decodeLength);//where we store temporary result (for appending)
	previous->currSeg=-1;//haven't added anything to it yet

	//save first word into active
	updateActiveWord(activeRead0,col0[c0]);
	updateActiveWord(activeRead1,col1[c1]);

	//nothing's been decoded yet
	activeAligned0->numSegs=0;
	activeAligned1->numSegs=0;

	while(c0<=sz0 && c1<=sz1){//keep going while still more words to process

		int cont0=0;
		int cont1=0;
		if(activeAligned0->currSeg>=activeAligned0->numSegs){//need to decode something in the first column
			cont0 = decodeNext(activeRead0,activeAligned0);//try to decode something
			if(cont0==-1){//need to update to finish decoding
				if(++c0<sz0) updateActiveWord(activeRead0,col0[c0]);//keep going
				else updateActiveWord(activeRead0,0);//fill it with zeros if there's nothing left
				cont0=decodeNext(activeRead0,activeAligned0);//keep decoding with new word
			}
		}
		if(activeAligned1->currSeg>=activeAligned1->numSegs){//need to decode something in the second column
			cont1 = decodeNext(activeRead1,activeAligned1);//try to decode something
			if(cont1==-1){//need to update to finished decoding
				if(++c1<sz1) updateActiveWord(activeRead1,col1[c1]);//keep going
				else updateActiveWord(activeRead1,0);//fill it with zeros if there's nothing left
				cont1=decodeNext(activeRead1,activeAligned1);//keep decoding with the new word
			}
		}
		if(cont0){//decoding caused read to be exhausted so need to move onto next word in column
			if(++c0<sz0) updateActiveWord(activeRead0,col0[c0]);
		}
		if(cont1){
			if(++c1<sz1) updateActiveWord(activeRead1,col1[c1]);
		}

		//now we go through each segment in the aligned segments, and them in the result, and append the result

		while(activeAligned0->currSeg<activeAligned0->numSegs && activeAligned1->currSeg<activeAligned1->numSegs){

			if(activeAligned0->flag[activeAligned0->currSeg]==1 && activeAligned1->flag[activeAligned1->currSeg]==1){//two fills
				word_32 runSize0 = getNumRuns(activeAligned0->seg[activeAligned0->currSeg],activeAligned0->length);
				word_32 runSize1 = getNumRuns(activeAligned1->seg[activeAligned1->currSeg],activeAligned1->length);

				if(runSize0<runSize1) fillANDfill(activeAligned0,activeAligned1,resultTemp);
				else fillANDfill(activeAligned1,activeAligned0,resultTemp);
			}
			else if(activeAligned0->flag[activeAligned0->currSeg]==1){//aligned0 is a fill, aligned1 is a lit
				fillANDlit(activeAligned0,activeAligned1,resultTemp);
			}
			else if(activeAligned1->flag[activeAligned1->currSeg]==1){//aligned1 is a fill, aligned0 is a lit
				fillANDlit(activeAligned1,activeAligned0,resultTemp);
			}
			else{//both literals
				litANDlit(activeAligned0,activeAligned1,resultTemp);
			}

			append(ret, &index, previous, resultTemp);//append it to the result that we are building
		}
	}

	//once we've gone through all words in both columns
	for(;previous->currSeg<previous->numSegs;previous->currSeg++){//clear out the rest of the previous
		previous->flag[previous->currSeg]=0;
		previous->seg[previous->currSeg]=0;
	}
	ret[index++] = createWord(previous);//save the last word into the result

	//free memory?
	freeActive(activeRead0);
	freeActive(activeRead1);
	freeActive(activeAligned0);
	freeActive(activeAligned1);
	freeActive(resultTemp);
	freeActive(previous);

	return index;
}


/**
 * Decodes the next word from read such that it is correctly aligned with the parameters of aligned
 */
int decodeNext(activeWord *read, activeWord *aligned){
	aligned->currSeg=0;
	if(aligned->length>read->length){//decode up
		int decoded = 0;
		if(read->flag[read->currSeg]==1 && aligned->decoding==0){//decoding run
			word_32 runSize = getNumRuns(read->seg[read->currSeg],read->length);
			word_32 transferRuns = runSize/(aligned->length/read->length);
			if(transferRuns > 0){//can fit into a decoded word
				aligned->seg[0] = transferRuns | ((read->seg[read->currSeg])>>(read->length-1))<<(aligned->length-1);
				aligned->numSegs = 1;
				aligned->flag[0] = 1;
				word_32 rem = runSize % (aligned->length/read->length);
				if(rem==0){//no remaining runs to decode
					read->currSeg++;
				}
				else if(rem==1){//turn into a lit for next time
					if(getFillType(read->seg[read->currSeg],read->length)==1){//runs of 1s
						read->seg[read->currSeg] = getOneLit(read->length);
					}
					else{
						read->seg[read->currSeg] = 0;
					}
					read->flag[read->currSeg] = 0;
					return 0;
				}
				decoded = aligned->length/read->length;
			}
			else{//these runs need to become literals
				word_32 pushSeg = 0;
				aligned->seg[0] = 0;
				aligned->flag[0] = 0;

				if(getFillType(read->seg[read->currSeg],read->length)==1){//run of 1s
					pushSeg = getOneLit(read->length);
				}
				for (decoded=0;decoded<runSize;decoded++){
					aligned->seg[0] <<= read->length;
					aligned->seg[0] |= pushSeg;
				}
				read->currSeg++;
				aligned->decoding=1;//mark it as a partial decoded word
				aligned->numSegs=decoded;
			}

		}
		else if(read->flag[read->currSeg]==0 && aligned->decoding==0){//this is the first segment but it's a literal
				aligned->decoding=1;
				aligned->numSegs=0;
				aligned->flag[0]=0;
				aligned->seg[0]=0;
		}
		if(aligned->decoding==1){
			while(aligned->numSegs<(aligned->length/read->length)){
				if(read->currSeg >= read->numSegs) return -1;//need to send it back to read a new segment
				aligned->seg[0] <<= read->length;//shift over to make room

				if(read->flag[read->currSeg]==0){//literal
					aligned->seg[0] |= read->seg[read->currSeg];//add the new literal
					read->currSeg++;//move on to next segment
				}
				else{//run of something
					if((read->seg[read->currSeg])>>(read->length-1)==1){//run of 1s
						aligned->seg[0] |= getOneLit(read->length);
					}
					read->seg[read->currSeg]--;
					word_32 runSize = getNumRuns(read->seg[read->currSeg],read->length);
					if(runSize==1){//only one run left so turn it into a literal for next time
						if(getFillType(read->seg[read->currSeg],read->length)){//run of 1s
							read->seg[read->currSeg] = getOneLit(read->length);
						}
						else{
							read->seg[read->currSeg] = 0;
						}
						read->flag[read->currSeg] = 0;
					}
				}
				aligned->numSegs++;//keep track of the new number of aligned segments
			}
			aligned->numSegs=1;
			aligned->decoding=0;
		}
	}
	else if(aligned->length<read->length){//decode down
		if(read->flag[read->currSeg]==0){//decoding literal
			aligned->numSegs=read->length/aligned->length;//number of segments in decoded will be how many times new length goes into old
			int i;
			for(i=0;i<aligned->numSegs;i++){//each new segment is equal to old segment shifted over by different bits accoring to i
				aligned->seg[i]= (read->seg[read->currSeg]<<(WORD_LENGTH-read->length+(i*aligned->length)))>>(WORD_LENGTH-(read->length-aligned->length));
				aligned->flag[i] = 0;
			}
			read->currSeg++;//mark that this segment is done
		}
		else{//decoding run
			word_32 runSize = getNumRuns(read->seg[read->currSeg],read->length);
			word_32 max = (word_32) pow(2,aligned->length-1) -1;//maximum number of runs the smaller length can represent
			word_32 runs = max/(read->length/aligned->length);//this is the maximum runs converted to the larger length
			if(runSize<=runs){//fits into one word
				aligned->seg[0] = (runSize*(read->length/aligned->length)) | ((read->seg[read->currSeg])>>(read->length-1))<<(aligned->length-1);
				read->currSeg++;
			}
			else{//subtract max
				aligned->seg[0] = (runs*(read->length/aligned->length)) | ((read->seg[read->currSeg])>>(read->length-1))<<(aligned->length-1);
				read->seg[read->currSeg]-=runs;
			}
			aligned->flag[0]=1;
			aligned->numSegs=1;
		}
	}
	else{//already aligned
		int i;
		for(i=0;i<read->numSegs;i++){
			aligned->flag[i]=read->flag[i];
			aligned->seg[i]=read->seg[i];
		}
		aligned->numSegs=read->numSegs;
		return 1;//exhausted
	}

	if(read->currSeg<read->numSegs) return 0;
	return 1;//returns whether read was exhausted
}


/**
 * Performs OR between col0 and col1 and saves result into ret
 */
int OR_VAL(word_32 *ret, word_32 *col0, int sz0, word_32 *col1, int sz1){
	int c0,c1,s0,s1, index;
	index = 0;//position of next word being written to the result
	c0=c1=1;
	s0=col0[0];
	s1=col1[0];
	word_32 decodeLength=s0;

	if(DECODE==DECODE_UP){
		if(s0<s1) decodeLength=s1;
	}
	else{
		if(s1<s0) decodeLength=s1;
	}

	activeWord *activeRead0 = initActiveWord(s0);
	activeWord *activeRead1 = initActiveWord(s1);
	activeWord *activeAligned0 = initActiveWord(decodeLength);
	activeWord *activeAligned1 = initActiveWord(decodeLength);
	activeWord *resultTemp = initActiveWord(decodeLength);

	activeWord *previous = initActiveWord(decodeLength);//where we store temporary result (for appending)
	previous->currSeg=-1;//haven't added anything to it yet

	//save first word into active
	updateActiveWord(activeRead0,col0[c0]);
	updateActiveWord(activeRead1,col1[c1]);

	//nothing's been decoded yet
	activeAligned0->numSegs=0;
	activeAligned1->numSegs=0;

	while(c0<=sz0 && c1<=sz1){//keep going while still more words to process
		int cont0=0;
		int cont1=0;
		if(activeAligned0->currSeg>=activeAligned0->numSegs){//need to decode something in the first column
			cont0 = decodeNext(activeRead0,activeAligned0);//try to decode something
			if(cont0==-1){//need to update to finish decoding
				if(++c0<sz0) updateActiveWord(activeRead0,col0[c0]);//keep going
				else updateActiveWord(activeRead0,0);//fill it with zeros if there's nothing left
				cont0=decodeNext(activeRead0,activeAligned0);//keep decoding with new word
			}
		}
		if(activeAligned1->currSeg>=activeAligned1->numSegs){//need to decode something in the second column
			cont1 = decodeNext(activeRead1,activeAligned1);//try to decode something
			if(cont1==-1){//need to update to finished decoding
				if(++c1<sz1) updateActiveWord(activeRead1,col1[c1]);//keep going
				else updateActiveWord(activeRead1,0);//fill it with zeros if there's nothing left
				cont1=decodeNext(activeRead1,activeAligned1);//keep decoding with the new word
			}
		}
		if(cont0){//decoding caused read to be exhausted so need to move onto next word in column
			if(++c0<sz0) updateActiveWord(activeRead0,col0[c0]);
		}
		if(cont1){
			if(++c1<sz1) updateActiveWord(activeRead1,col1[c1]);
		}

		//now we go through each segment in the aligned segments, and them in the result, and append the result
		while(activeAligned0->currSeg<activeAligned0->numSegs && activeAligned1->currSeg<activeAligned1->numSegs){
			if(activeAligned0->flag[activeAligned0->currSeg]==1 && activeAligned1->flag[activeAligned1->currSeg]==1){//two fills
				word_32 runSize0 = getNumRuns(activeAligned0->seg[activeAligned0->currSeg],activeAligned0->length);
				word_32 runSize1 = getNumRuns(activeAligned1->seg[activeAligned1->currSeg],activeAligned1->length);
				if(runSize0<runSize1) fillORfill(activeAligned0,activeAligned1,resultTemp);
				else fillORfill(activeAligned1,activeAligned0,resultTemp);
			}
			else if(activeAligned0->flag[activeAligned0->currSeg]==1){//aligned0 is a fill, aligned1 is a lit
				fillORlit(activeAligned0,activeAligned1,resultTemp);
			}
			else if(activeAligned1->flag[activeAligned1->currSeg]==1){//aligned1 is a fill, aligned0 is a lit
				fillORlit(activeAligned1,activeAligned0,resultTemp);
			}
			else{//both literals
				litORlit(activeAligned0,activeAligned1,resultTemp);
			}
			append(ret, &index, previous, resultTemp);//append it to the result that we are building
		}
	}

	//once we've gone through all words in both columns
	for(;previous->currSeg<previous->numSegs;previous->currSeg++){//clear out the rest of the previous
		previous->flag[previous->currSeg]=0;
		previous->seg[previous->currSeg]=0;
	}
	ret[index++] = createWord(previous);//save the last word into the result

	//free memory?
	freeActive(activeRead0);
	freeActive(activeRead1);
	freeActive(activeAligned0);
	freeActive(activeAligned1);
	freeActive(resultTemp);
	freeActive(previous);

	return index;
}
