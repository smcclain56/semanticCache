
#ifndef VALQUERY_H_
#define VALQUERY_H_

#include "Control.h"
#include "ActiveWord.h"

void initVALQuery();


int AND_VAL(word_32 *, word_32 *,int, word_32 *,int);

int OR_VAL(word_32 *,word_32 *,int,word_32 * ,int);

int decodeNext(activeWord *,activeWord *);

#endif /* VALQUERY_H_ */
