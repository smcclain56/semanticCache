/*
 * Query.h
 *
 *  Created on: Jun 14, 2016
 *      Author: alexia
 */
#include "Control.h"
#include "QueryData.h"
#include "hashData.h"

#ifndef QUERY_H_
#define QUERY_H_

#define LSIZE 256
#define MAX_NUM_RANGES 100
#define MAX_NUM_CHAR 10
#define NUM_LINES 5


//MAIN METHODS
void executeRange(queryData *results, char *bitmapFile, int start, int end, struct list **hash, word_32 **cols, int *sz);
void cache_search(queryData *ret, char *bitmapFile, int start, int end, unsigned int key, struct list **hash, word_32 **cols, int *sz, int column_bits);
word_32* getRemainderQuery(queryData *results, char *bitmapFile, int start_stored, int end_stored, int start_wanted, int end_wanted, struct list **hash, word_32 **cols, int *sz, int column_bits);
void runQueries(char *bitmapFile, char *queryFle);
void freeCol(word_32 **cols, int num_columns);
void setupColumns(queryData *ret, char *bitmapFile, int start, int end, word_32 **cols, int *sz);
void loadCol(char *bitmapFile, int column, word_32 **cols, int *sz);


//HELPER METHODS
int tokenize(char *line, char instruction[MAX_NUM_RANGES][MAX_NUM_CHAR]);
void initSize(int *sz, int num_columns);
int getNumCols(char *bitmapFile, char *queryFile);
//void printCol(char *bitmapFile, int column, word_32 **cols, int *sz);
void printCols(int column, word_32** cols, int *sz);
void buildQueryOutput(char *queryFile, int num_columns);


//REMAINDER QUERY HELPER METHODS
queryData* chop_endW_to_endS(queryData *results, int end_stored, int end_wanted, int start_wanted, struct list **hash, word_32 **cols, int *sz);
queryData* chop_startS_to_startW(queryData *results, int start_stored, int start_wanted, int end_wanted, struct list **hash, word_32 **cols, int *sz);
queryData* add_startW_to_startS(queryData *results, int start_wanted, int start_stored, struct list **hash, word_32 **cols, int *sz);
queryData* add_endS_to_endW(queryData* results, int end_stored, int end_wanted, struct list **hash,  word_32 **cols, int *sz);


#endif /* QUERY_H_ */
