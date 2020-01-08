#include <stdio.h>
#include <stdlib.h>
#include "WAHQuery.h"
#include "SegUtil.h"


/*
 * ANDs two columns together (col0 AND col1) and saves into ret arg
 * sz0 and sz1 are sizes of the columns we're ANDing
 */


int AND_WAH(word_32 *ret, word_32 *col0, int sz0, word_32 *col1, int sz1){

	int c0;//the word number we're scanning from col0
	int c1;//the word number we're scanning from col1
	int d;//the spot we're saving into the result
	c0 = c1 = 1;//track which word we're looking at
	d=0;//start saving into the first spot

	word_32 w0 = col0[c0++];//get the first word from first col
	word_32 w1 = col1[c1++];//get the first word from second col


	//get each of their word type SegUtil.h type definitions
	int t0 = getType(w0,WORD_LENGTH);
	int t1 = getType(w1,WORD_LENGTH);

	while(c0<=sz0 && c1<=sz1){
		word_32 toAdd;//this is the result word we're creating from w0 and w1
		if(t0 < ZERO_RUN && t1 < ZERO_RUN){//two literals
			toAdd = litANDlitWAH(w0,w1);
			//update both
			w0 = col0[c0++];
			t0 = getType(w0,WORD_LENGTH);
			w1 = col1[c1++];
			t1 = getType(w1,WORD_LENGTH);
		}
		else if(t0 < ZERO_RUN || t1 < ZERO_RUN){//one literal, one fill
			if(t0 < ZERO_RUN){//w0 is the literal
				toAdd = fillANDlitWAH(&w1,&t1,w0);
				w0 = col0[c0++];//update the literal
				t0 = getType(w0,WORD_LENGTH);
			}
			else{//w1 is the literal
				toAdd = fillANDlitWAH(&w0,&t0,w1);
				w1 = col1[c1++];//update the literal
				t1 = getType(w1,WORD_LENGTH);
			}
		}
		else{//two fills
			 if ((w0 << 2) < (w1 << 2)){//w0 is smaller
				toAdd = fillANDfillWAH(w0,t0,&w1,&t1);
				w0 = col0[c0++];//update the smaller fill
				t0 = getType(w0,WORD_LENGTH);
			 }
			 else if ((w0 << 2) > (w1 << 2)){//w1 is smaller
				toAdd = fillANDfillWAH(w1,t1,&w0,&t0);
				w1 = col1[c1++];//update the smaller fill
				t1 = getType(w1,WORD_LENGTH);
			 }
			 else{//special case, equal fills (can be treated as literals)
				 toAdd = litANDlitWAH(w0,w1);
				 //update both
				 w0 = col0[c0++];
				 t0 = getType(w0,WORD_LENGTH);
				 w1 = col1[c1++];
				 t1 = getType(w1,WORD_LENGTH);
			 }
		}
		if(d>=1){//if this isn't the first word, append it to the end of the resulting column we're building
			appendWAH(ret,toAdd,&d);
		}
		else{//special case --> first word (can't append because first word is wordLength)
			ret[++d] = toAdd;//just add it //TODO THIS IS CAUSING SEG FAULT -- I changed this from ret[++d]
			}
		}

	return d+1;//the number of words we just wrote
}


/*
 * ORs two columns together (col0 AND col1) and saves into ret arg
 * sz0 and sz1 are sizes of the columns we're ORing
 */
int OR_WAH(word_32 *ret, word_32 *col0, int sz0, word_32 *col1, int sz1){
		//printf("\tin OR_WAH ret=%p\n",ret );
		int c0=1;//track which word in col0 we're looking at
		int c1=1;//track which word in col1 we're looking at
		int d=0;//track which spot in the resulting array we're saving into

		//get the first word from each column
		word_32 w0 = col0[c0++];
		word_32 w1 = col1[c1++];

		//and figure out their types (see type definition in SegUtil.h)
		int t0 = getType(w0,WORD_LENGTH);
		int t1 = getType(w1,WORD_LENGTH);
		while(c0<=sz0 && c1<=sz1){
			word_32 toAdd;//this is the resulting word from ORing w0 and w1
			if(t0 < ZERO_RUN && t1 < ZERO_RUN){//two literals
				toAdd = litORlitWAH(w0,w1);
				//update both
				w0 = col0[c0++];
				t0 = getType(w0,WORD_LENGTH);
				w1 = col1[c1++];
				t1 = getType(w1,WORD_LENGTH);
			}
			else if(t0 < ZERO_RUN || t1 < ZERO_RUN){//one literal, one fill
				if(t0 < ZERO_RUN){//w0 is the literal
					toAdd = fillORlitWAH(&w1,&t1,w0);
					w0 = col0[++c0];//update the literal //TODO I CHANGED THIS FROM c0++
					t0 = getType(w0,WORD_LENGTH);
				}
				else{//w1 is the literal
					toAdd = fillORlitWAH(&w0,&t0,w1);
					w1 = col1[c1++];//update the literal
					t1 = getType(w1,WORD_LENGTH);
				}
			}
			else{//two fills
				 if ((w0 << 2) < (w1 << 2)){//w0 is smaller
					toAdd = fillORfillWAH(w0,t0,&w1,&t1);
					w0 = col0[c0++];//update the smaller fill
					t0 = getType(w0,WORD_LENGTH);
				 }
				 else if ((w0 << 2) > (w1 << 2)){//w1 is smaller
					toAdd = fillORfillWAH(w1,t1,&w0,&t0);
					w1 = col1[c1++];//update the smaller
					t1 = getType(w1,WORD_LENGTH);
				 }
				 else{//special case, equal fills (can be treated as literals)
					 toAdd = litORlitWAH(w0,w1);
					 //update both
					 w0 = col0[c0++];
					 t0 = getType(w0,WORD_LENGTH);
					 w1 = col1[c1++];
					 t1 = getType(w1,WORD_LENGTH);
				 }
			}
			if(d>=1){//if we're in the middle of the columns, just append
				appendWAH(ret,toAdd,&d); //TODO THIS LINE IS CAUSING ERROR
			}
			else{//if we're on the first word, can't append (first word is wordLength)
				ret[++d] = toAdd;
			}
		}
		return d+1;//the total number of words result now has
}


/*
 * Adds the wordToAdd to the end (d=last added position) of the addTo sequence
 * wordToAdd will be consolidated into position if possible and if not (or the leftover) will go into d+1
 */
// void appendWAH(word_32 *addTo, word_32 wordToAdd, int *d)
// {
// 		// printf("\tappendWAH\n" );
// 		// printColsWah(cols);
//
// 	int prevT = getType(addTo[*d],WORD_LENGTH);//type of the last added
//
// 	if(prevT==LITERAL){//there's no way to consolidate
// 		addTo[(*d)++] = wordToAdd;
// 			//printf("\t1\n" );
// 			//printColsWah(cols);
// 		return;
// 	}
// 	int addT = getType(wordToAdd,WORD_LENGTH);//type of the one we're adding
//
// 	if(prevT==ZERO_RUN){
// 		if(addT == ZERO_RUN){//both zero runs so we might be able to consolidate if there's enough room
// 			word_32 minCheck = getZeroFill(BASE_LEN)-2;//helps to check the stopping condition (as long as there are still fills left)
// 			word_32 maxCheck = getMaxZeroFill(BASE_LEN);
// 			while(wordToAdd>minCheck && addTo[(*d)]<maxCheck){
// 				addTo[(*d)]++;  //
//
// 				wordToAdd--;
// 			}
// 			if(wordToAdd==minCheck){
// 				//	printf("\t2\n" );
// 				//	printColsWah(cols);
// 				return;//successfully added everything to previous
// 			}
// 			else{//stopped because ran out of space
// 				if(wordToAdd==minCheck+1){//there was exactly one left so switch to literal before adding
// 					wordToAdd = 0;
// 				}
// 				addTo[++(*d)] = wordToAdd;
// 				//	printf("\t3\n" );
// 				//	printColsWah(cols);
// 				return;
// 			}
// 		}
// 		else if(addT == ZERO_LIT){//we can probably just add this one lit to the previous run
// 			if(addTo[(*d)]<getMaxZeroFill(BASE_LEN)){//not maxed out yet, so just add it
// 				addTo[(*d)]++;
// 			}
// 			else{//maxed out so can't consolidate
// 				addTo[++(*d)] = wordToAdd;//save into the next spot
// 				//	printf("\t4\n" );
// 				//	printColsWah(cols);
// 				return;
// 			}
// 		}
// 		else{//can't consolidate
// 			addTo[(*d)++] = wordToAdd; //NOTE: THIS LINE USED TO BE addTo[++(*d)] = wordToAdd
// 				// printf("\t5\n" );
// 				// printColsWah(cols);
// 			return;
// 		}
// 	}
// 	else if(prevT==ZERO_LIT){
// 		if(addT==ZERO_LIT){//consolidate two literals into one fill
// 			addTo[++(*d)] = getZeroFill(BASE_LEN); //NOTE I ADDED THE ++ ON THIS LINE
// 				// printf("\t6\n" );
// 				// printColsWah(cols);
// 			return;
// 		}
// 		else if(addT==ZERO_RUN){
// 			if(wordToAdd<getMaxZeroFill(BASE_LEN)){//not maxed out yet, so add it
// 				addTo[(*d)] = wordToAdd+1;
// 				//	printf("\t7\n" );
// 				//	printColsWah(cols);
// 				return;
// 			}
// 			else{//maxed out
// 				addTo[(*d)+1] = addTo[(*d)];
// 				addTo[(*d)++] = wordToAdd;
// 				//	printf("\t8\n" );
// 				//	printColsWah(cols);
// 				return;
// 			}
// 		}
// 		else{//can't consolidate
// 			addTo[(*d)++] = wordToAdd; //NOTE I CHANGED THIS from addTo[++(*d)]
// 			//	printf("\t9\n" );
// 			//	printColsWah(cols);
// 			return;
// 		}
//
// 	}
// 	else if(prevT==ONE_RUN){
// 		if(addT == ONE_RUN){//both run of ones so might be able to consolidate
// 			word_32 minCheck = getOneFill(BASE_LEN)-2;//helps to check the stopping condition (as long as there are still fills left)
// 			word_32 maxCheck = getMaxOneFill(BASE_LEN);
// 			while(wordToAdd>minCheck && addTo[(*d)]<maxCheck){
// 				addTo[(*d)]++;
// 				wordToAdd--;
// 			}
// 			if(wordToAdd==minCheck){
// 				//	printf("\t10\n" );
// 				//	printColsWah(cols);
// 				return;//successfully added everything to previous
// 			}
// 			else{//stopped because ran out of space
// 				if(wordToAdd==minCheck+1){//there was exactly one left so switch to literal before adding
// 					wordToAdd = getZeroFill(BASE_LEN)-3;
// 				}
// 				addTo[++(*d)] = wordToAdd;
// 				//	printf("\t11\n" );
// 				//	printColsWah(cols);
// 				return;
// 			}
//
// 		}
// 		else if(addT == ONE_LIT){
// 			if(addTo[(*d)]<getMaxOneFill(BASE_LEN)){//previous isn't maxed out so just add it
// 				addTo[(*d)]++;
// 				//	printf("\t12\n" );
// 				//	printColsWah(cols);
// 				return;
// 			}
// 			else{//maxed out so can't consolidate
// 				addTo[++(*d)] = wordToAdd;
// 				//	printf("\t13\n" );
// 				//	printColsWah(cols);
// 				return;
// 			}
// 		}
// 		else{
// 			addTo[++(*d)] = wordToAdd;
// 			//	printf("\t14\n" );
// 			//	printColsWah(cols);
// 			return;
// 		}
// 	}
// 	else{//prev is lit of ones
// 		if(addT==ONE_LIT){
// 			addTo[(*d)] = getOneFill(BASE_LEN);
// 			//	printf("\t15\n" );
// 			//	printColsWah(cols);
// 			return;
// 		}
// 		else if(addT==ONE_RUN){
// 			if(wordToAdd<getMaxOneFill(BASE_LEN)){//not maxed out
// 				addTo[(*d)] = wordToAdd+1;
// 			}
// 			else{//maxed out so can't consolidate
// 				addTo[(*d)+1] = addTo[(*d)];
// 				addTo[(*d)++] = wordToAdd;
// 				//	printf("\t16\n" );
// 				//	printColsWah(cols);
// 				return;
// 			}
// 		}
// 		else{//can't consolidate
// 			addTo[++(*d)] = wordToAdd;
// 			//	printf("\t17\n" );
// 			//	printColsWah(cols);
// 			return;
// 		}
// 	}
// }

void appendWAH(word_32 *addTo, word_32 wordToAdd, int *d)
{
	int prevT = getType(addTo[*d],WORD_LENGTH);//type of the last added

	if(prevT==LITERAL){//there's no way to consolidate
		addTo[(*d)++] = wordToAdd;
		return;
	}
	int addT = getType(wordToAdd,WORD_LENGTH);//type of the one we're adding

	if(prevT==ZERO_RUN){
		if(addT == ZERO_RUN){//both zero runs so we might be able to consolidate if there's enough room
			word_32 minCheck = getZeroFill(BASE_LEN)-2;//helps to check the stopping condition (as long as there are still fills left)
			word_32 maxCheck = getMaxZeroFill(BASE_LEN);
			while(wordToAdd>minCheck && addTo[(*d)]<maxCheck){
				addTo[(*d)]++;  //
				wordToAdd--;
			}
			if(wordToAdd==minCheck){
				return;//successfully added everything to previous
			}
			else{//stopped because ran out of space
				if(wordToAdd==minCheck+1){//there was exactly one left so switch to literal before adding
					wordToAdd = 0;
				}
				addTo[(*d)++] = wordToAdd;
				return;
			}
		}
		else if(addT == ZERO_LIT){//we can probably just add this one lit to the previous run
			if(addTo[(*d)]<getMaxZeroFill(BASE_LEN)){//not maxed out yet, so just add it
				addTo[(*d)]++;
			}
			else{//maxed out so can't consolidate
				addTo[(*d)++] = wordToAdd;//save into the next spot
				return;
			}
		}
		else{//can't consolidate
			addTo[(*d)++] = wordToAdd; //NOTE: THIS LINE USED TO BE addTo[++(*d)] = wordToAdd
			return;
		}
	}
	else if(prevT==ZERO_LIT){
		if(addT==ZERO_LIT){//consolidate two literals into one fill
			addTo[(*d)++] = getZeroFill(BASE_LEN); //NOTE I ADDED THE ++ ON THIS LINE
			return;
		}
		else if(addT==ZERO_RUN){
			if(wordToAdd<getMaxZeroFill(BASE_LEN)){//not maxed out yet, so add it
				addTo[(*d)] = wordToAdd+1;
				return;
			}
			else{//maxed out
				addTo[(*d)+1] = addTo[(*d)];
				addTo[(*d)++] = wordToAdd;
				return;
			}
		}
		else{//can't consolidate
			addTo[(*d)++] = wordToAdd; //NOTE I CHANGED THIS from addTo[++(*d)]
			return;
		}

	}
	else if(prevT==ONE_RUN){
		if(addT == ONE_RUN){//both run of ones so might be able to consolidate
			word_32 minCheck = getOneFill(BASE_LEN)-2;//helps to check the stopping condition (as long as there are still fills left)
			word_32 maxCheck = getMaxOneFill(BASE_LEN);
			while(wordToAdd>minCheck && addTo[(*d)]<maxCheck){
				addTo[(*d)]++;
				wordToAdd--;
			}
			if(wordToAdd==minCheck){
				return;//successfully added everything to previous
			}
			else{//stopped because ran out of space
				if(wordToAdd==minCheck+1){//there was exactly one left so switch to literal before adding
					wordToAdd = getZeroFill(BASE_LEN)-3;
				}
				addTo[(*d)++] = wordToAdd;
				return;
			}

		}
		else if(addT == ONE_LIT){
			if(addTo[(*d)]<getMaxOneFill(BASE_LEN)){//previous isn't maxed out so just add it
				addTo[(*d)]++;
				return;
			}
			else{//maxed out so can't consolidate
				addTo[(*d)++] = wordToAdd;
				return;
			}
		}
		else{
			addTo[(*d)++] = wordToAdd;
			return;
		}
	}
	else{//prev is lit of ones
		if(addT==ONE_LIT){
			addTo[(*d)] = getOneFill(BASE_LEN);
			return;
		}
		else if(addT==ONE_RUN){
			if(wordToAdd<getMaxOneFill(BASE_LEN)){//not maxed out
				addTo[(*d)] = wordToAdd+1;
			}
			else{//maxed out so can't consolidate
				addTo[(*d)+1] = addTo[(*d)];
				addTo[(*d)++] = wordToAdd;
				return;
			}
		}
		else{//can't consolidate
			addTo[(*d)++] = wordToAdd;
			return;
		}
	}

}

/* Performs an OR on 2 fills
 * Updates the larger fill for the future
 * Returns the resulting word
 */
word_32 fillORfillWAH(word_32 smallFill, int smallT, word_32 *bigFill, int *bigT){
	word_32 ret;
	word_32 sub = ((smallFill<<2)>>2);//what we are going to subtract from the larger

	if(smallT == ZERO_RUN && *bigT == ZERO_RUN){//if both 0 runs, must return small run of 0s
		ret = smallFill;
	}
	else{
		ret = getOneFill(BASE_LEN)-2+sub;//build run of 1s of length sub (number of runs in smallFill)
	}
	*bigFill -= sub;//subtract that from the larger

	//check to see if we subtracted too much
	if(((*bigFill<<2)>>2)==1){//need to change to literal
		if(*bigT==ZERO_RUN){
			*bigFill = 0;//literal run of zeros
			*bigT = ZERO_LIT;
		}
		else{//update the larger fill for the future
			*bigFill = getZeroFill(BASE_LEN)-3;//literal run of ones
			*bigT = ONE_LIT;
		}
	}

	return ret;
}

/*
 * Performs an AND on 2 fills
 * Updates the larger of the fills for the future
 * Returns the resulting word
 */
word_32 fillANDfillWAH(word_32 smallFill, int smallT, word_32 *bigFill, int *bigT){
	word_32 ret;
	word_32 sub = ((smallFill<<2)>>2);//what we are going to subtract from the larger (number of runs the smallFill represents)

	if(smallT == ONE_RUN && *bigT == ONE_RUN){//the only way to return run of 1s is if both are 1s
		ret = smallFill;
	}
	else{//otherwise we're returning a run of zeros of length smallFill was
		ret = getZeroFill(BASE_LEN)-2+sub;//build run of 0s of length sub (number of runs in smallFill)
	}

	*bigFill -= sub;

	if(((*bigFill<<2)>>2)==1){
		if(*bigT==ZERO_RUN){
			*bigFill = 0;//literal run of zeros
			*bigT = ZERO_LIT;
		}
		else{
			*bigFill = getZeroFill(BASE_LEN) -3;//literal run of ones
			*bigT = ONE_LIT;
		}
	}

	return ret;
}


/*
 * Performs an OR on a fill and a literal word
 * Updates the fill for the future
 * Returns the resulting word
 */
word_32 fillORlitWAH(word_32 *fill, int *fillT, word_32 lit){
	word_32 ret;//the word to be returned

	if(*fillT == ONE_RUN){//whatever we or with a run of 1s will be 1 so just return lit of ones (later)
		ret = getZeroFill(BASE_LEN) -3;//literal run of 1s
	}
	else{//otherwise we have a run of 1s so the result will be whatever the literal is
		ret = lit;
	}

	if(((*fill<<2)>>2)==2){//we need to turn it into a literal
		if(*fillT == ZERO_RUN){
			*fill = 0;//literal run of 0s
			*fillT = ZERO_LIT;
		}
		else{
			*fill = getZeroFill(BASE_LEN)-3;//literal run of ones
			*fillT = ONE_LIT;
		}
	}
	else{//otherwise we can decrement without a problem
		*fill -= 1;
	}
	return ret;
}

/*
 * Performs an AND on a fill and a literal
 * Updates the fill for the future
 * Returns the resulting word
 */
word_32 fillANDlitWAH(word_32 *fill, int *fillT, word_32 lit){
	word_32 ret;
	if(*fillT == ZERO_RUN){//whatever we and with a run of 0s will be 0 so just return 0 (later)
		ret = 0;
	}
	else{//otherwise we have a run of 1s so the result will be whatever the literal is
		ret = lit;
	}

	if(((*fill<<2)>>2)==2){//we need to turn it into a literal
		if(*fillT == ZERO_RUN){
			*fill = 0;//literal run of 0s
			*fillT = ZERO_LIT;
		}
		else{
			*fill = getZeroFill(BASE_LEN)-3;//literal run of ones
			*fillT = ONE_LIT;
		}
	}
	else{//otherwise we can decrement without a problem
		*fill -= 1;
	}
	return ret;
}

/*
 * Performs an OR between 2 literals and returns the resulting word
 */
word_32 litORlitWAH(word_32 lit1, word_32 lit2){
	return lit1 | lit2;//just or them together
}

/*
 * Performs an AND between 2 literals and returns the resulting word
 */
word_32 litANDlitWAH(word_32 lit1, word_32 lit2){
	return lit1 & lit2;//just and them together
}
