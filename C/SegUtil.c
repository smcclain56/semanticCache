#include "SegUtil.h"

word_32 one = 1;//used for bitwise operations with longs/ints
word_32 oneLit[4];//4 different combinations (3 lengths for VAL, 1 for WAH)
word_32 maxZeroFill[4];
word_32 maxOneFill[4];
word_32 zeroFill[4];
word_32 oneFill[4];

int compression;


/**
 * Builds and returns minimum run of 0s (2) of length=len : 100000....10
 * Len = number of bits to occupy total
 */
word_32 createZeroFill(word_32 len){
	word_32 ret = (0 | (one<<(len-one)))+2;
	return ret;
}

/**
 * Builds and returns a maxed out run of 0s of length=len : (101111...11)
 * Len = number of bits to occupy total
 */
word_32 createMaxZeroFill(word_32 len){
	word_32 ret = 0;
	int i;
	ret |= (one<<(len-one));

	for(i=len-3;i>=0;i--){
		ret |= (one<<i);
	}

	return ret;
}

/**
 * Builds and returns a maxed out run of 1s of length=len : (111111...11)
 * Len = number of bits to occupy total
 */
word_32 createMaxOneFill(word_32 len){
	word_32 ret = 0;
	int i;
	for(i=len-1;i>=0;i--){
		ret |= (one<<i);
	}
	return ret;
}

/**
 * Builds and returns literal of 1s of length=len : (01111..1111)
 * Len = number of bits to occupy total
 */
word_32 createOneLit(word_32 len){
	int j;
	word_32 ret = 0;
	for(j=0;j<len-1;j++){
		ret |= (one<<j);
	}
	return ret;
}

/*
 * Builds and returns minimum run of 1s (2) of length=len : 110000....10
 * Len = number of bits to occupy total
 */
word_32 createOneFill(word_32 len){
	//word_32 one = 1;
	word_32 ret =  getZeroFill(len) | (one<<(len-2));

	return ret;
}


/**
 * Returns minimum zero fill of specified length
 */
word_32 getZeroFill(word_32 len){
	if(compression == VAL) return zeroFill[(WORD_LENGTH/len)-1];
	return zeroFill[2];
}

/**
 * Returns minimum one fill of specified length
 */
word_32 getOneFill(word_32 len){
	if(compression == VAL) return oneFill[(WORD_LENGTH/len)-1];
	return oneFill[2];
}

/**
 * Returns maximum zero fill of specified length
 */
word_32 getMaxZeroFill(word_32 len){
	if(compression == VAL) return maxZeroFill[(WORD_LENGTH/len)-1];
	return maxZeroFill[2];
}

/**
 * Returns maximum one fill of specified length
 */
word_32 getMaxOneFill(word_32 len){
	if(compression == VAL) return maxOneFill[(WORD_LENGTH/len)-1];
	return maxOneFill[2];
}

/**
 * Returns literal of ones of specified length
 */
word_32 getOneLit(word_32 len){
	if(compression == VAL) return oneLit[(WORD_LENGTH/len)-1];
	return oneLit[2];
}

/*
 * Returns type of word (see int constants in header) -- really only used for WAH
 */
int getType(word_32 word, word_32 len){
	if(word == 0x0) return ZERO_LIT;

	if(word == getOneLit(len)) return ONE_LIT;

	if(word>>(len-1) & one){
		if(word>>(len-2) & one){
			return ONE_RUN;
		}
		else{
			return ZERO_RUN;
		}
	}
	return LITERAL;
}

/**
 * Returns number of runs represented in segment (assumed is fill)
 */
word_32 getNumRuns(word_32 word,word_32 len){
	return (word<<(WORD_LENGTH-len+1))>>(WORD_LENGTH-len+1);//number of runs
}

/**
 * Returns which type of fill this word is (assumed is fill)
 */
word_32 getFillType(word_32 word,word_32 len){
	return word>>(len-1);
}

/**
 * Initializes all built segments into array of possibilities
 * seg[0] = VAL with 1 segment/word (28/60)
 * seg[1] = VAL with 2 segments/word (14/30)
 * seg[2] = WAH 32/64 (depending on WORD_LENGTH)
 * seg[3] = VAL with 4 segments/word (7/15)
 */
void initUtilSegs(int comp){
	compression=comp;
	if(compression==WAH){//only need to fill index 2
		oneLit[2]=createOneLit(WORD_LENGTH);
		maxZeroFill[2]=createMaxZeroFill(WORD_LENGTH);
		maxOneFill[2]=createMaxOneFill(WORD_LENGTH);
		zeroFill[2]=createZeroFill(WORD_LENGTH);
		oneFill[2]=createOneFill(WORD_LENGTH);
	}
	else{
		int i;
		for(i=0;i<4;i++){
			if(i==2) continue;//don't care about WAH place
			int length = ((WORD_LENGTH-FLAG_BITS)/(i+1)) + 1;
 			oneLit[i]=createOneLit(length);
			maxZeroFill[i]=createMaxZeroFill(length);
			maxOneFill[i]=createMaxOneFill(length);
			zeroFill[i]=createZeroFill(length);
			oneFill[i]=createOneFill(length);
		}
	}
}
