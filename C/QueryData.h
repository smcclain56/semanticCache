/*
 * QueryData.h
 *
 *  Created on: Jun 14, 2016
 *      Author: alexia
 */

#ifndef QUERYDATA_H_
#define QUERYDATA_H_

//struct to help in querying data
typedef struct queryData{
	word_32 *result;
	int resultSize;
	int complete_cache; //this variable checks if complete query was found or was formed with within lookup 1=complete cached, 0=formed during lookup (need to add to hash)
} queryData;



#endif /* QUERYDATA_H_ */
