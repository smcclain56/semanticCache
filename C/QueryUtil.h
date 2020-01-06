/*
 * QueryUtil.h
 *
 *  Created on: Jul 1, 2016
 *      Author: alexia
 */

#ifndef QUERYUTIL_H_
#define QUERYUTIL_H_

#include "ActiveWord.h"

void fillORfill(activeWord *, activeWord *,activeWord *);
void litORlit(activeWord *, activeWord *, activeWord *);
void fillORlit(activeWord *, activeWord *,activeWord *);

void fillANDfill(activeWord *, activeWord *, activeWord *);
void litANDlit(activeWord *, activeWord *, activeWord *);
void fillANDlit(activeWord *, activeWord *, activeWord *);

void append(word_32 *, int *, activeWord *, activeWord *);

#endif /* QUERYUTIL_H_ */
