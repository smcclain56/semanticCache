
#ifndef HASHDATA_H_
#define HASHDATA_H_

#include "Control.h"
//structs
struct node{
  unsigned int key;
  word_32 *val;
  struct node* next;
  struct node* prev;
};

struct list{
  int size;
  struct node *head;
  struct node *tail;
};

#endif /* QUERYDATA_H_ */
