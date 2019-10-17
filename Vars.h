/*
 * Vars.h
 *
 *  Created on: Jun 14, 2016
 *      Author: alexia
 */

#ifndef VARS_H_
#define VARS_H_



//this file is used to define the parameters
#define WAH 0
#define VAL 1
#define UNSTRIPED 0
#define STRIPED 1
#define IN_CORE 0
#define OUT_CORE 1
#define DECODE_UP 0//run VAL decode up algorithm when querying
#define DECODE_DOWN 1//run VAL decode down algorithm when querying

#define HASH_MAP 0 //run hashmap when caching
#define NO_CACHE 1 //run without caching
#define JARVIS 0 //run using jarvis_lookup method
#define FIND_SPLIT 1 //run using find and split lookup method

#define BUFF_SIZE 300//for file name buffers
#define CONV pow(10,6)

#endif /* VARS_H_ */
