
#include "BlockSeg.h"

#ifndef RAWBITMAPREADER_H_
#define RAWBITMAPREADER_H_


//char *readFile(char[]);
void runOverhead(int striped);


double compress(char *, int, int, int);
int initCompression(int striped);
void clearMem(int striped);



//Unstriped methods
void compressUnstriped();
void *compressNext(void *);
void compressColumn(int, int);

//Striped methods
void compressStriped();
//void *startThread(void *);
void *startThreadStriped(void *);
//int readFirst();
//int readNext();
//void prepCompressBlock(int);
//int readStriped();
//void *compressCol(void *);

int readNextBlock(FILE *, int, int, blockSeg **);

void printQ();

//Helper methods for managing folder names
//char *getDir(char[], char[]);
//char *getPath(char[]);
//char *folderName();
//char *getReadingFileName(int);
//char *getCompressedFileName(int);

#endif /* RAWBITMAPREADER_H_ */
