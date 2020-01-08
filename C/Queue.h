/* queue.h */

// define queue's data members
#include "hashData.h"

/* Queue function prototypes */
struct list *newqueue(void);
void printqueue(struct list *q);
unsigned int	removeEntry(unsigned int key, struct list *q);
int	isempty(struct list *q);
unsigned int	dequeue(struct list *q);
unsigned int	enqueue(struct node *toAdd, struct list *q);
