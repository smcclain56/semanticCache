#ifndef WRITER_H_
#define WRITER_H_


#include "Control.h"

int reformat(char **);
int toUnstriped();
int toStriped(int);
int readRow(FILE *,int *,int);
char *unstripedExt(char *);
char *stripedExt(char *,int);

#endif /* WRITER_H_ */
