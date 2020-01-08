/* queue.c - enqueue, dequeue, isempty, nonempty, et al. */

#include <stdlib.h>
#include <stdio.h>
#include "Queue.h"
#include "Control.h"


struct list *newqueue(void)
{
	// allocate memory for a new Queue structure (required to be on heap)
	struct list *newQ = (struct list*)malloc(sizeof(struct list));

	//  initialize the structure
	newQ-> head = NULL;
	newQ-> tail = NULL;
	newQ-> size = 0;

	//return pointer to the structure
	return newQ;
}

/**
 * Prints out contents of a queue
 * @param q	pointer to a queue
 */
void	printqueue(struct list *q)
{
	//print all contents from head to tail
	struct node* current;
	if(isempty(q)){
		return;
	}
	current = q->head;
	int i = 0;
	while(current != NULL){
		printf("QueueEntry %d = key= %u \n",i, current->key);
		current = current->next;
		i++;
	}
}

/**
 * Checks whether queue is empty
 * @param q	Pointer to a queue
 * @return 1 if true, 0 otherwise
 */
int	isempty(struct list *q)
{
	if(q == NULL){
		return 1; //true
	}
	if(q->size == 0){
		return 1; //true
	}
	return 0; //false

}

/**
 * Insert a process at the tail of a queue
 * @param pid	ID process to insert
 * @param q	Pointer to the queue to use
 *
 * @return pid on success, SYSERR otherwise
 */
unsigned int enqueue(struct node *toAdd, struct list *q)
{
	//allocate space on heap for a new QEntry
   //initialize the new QEntry
	struct node* newTail = (struct node*)malloc(sizeof(struct node));
	newTail->key = toAdd->key;
  newTail->val = toAdd->val;

	if(isempty(q)){
		q->head = newTail;
		q->tail = newTail;
		q->size+=1;
		newTail->next = NULL;
		newTail->prev = NULL;
		return newTail->key;
	}

	//insert into tail of queue
	q-> tail-> next = newTail;
	newTail-> prev = q-> tail;
	newTail-> next = NULL;
	q-> tail = newTail;

	//return the pid on success
	q->size +=1;
	return newTail->key;

}

/**
 * Remove and return the first process on a list
 * @param q	Pointer to the queue to use
 * @return pid of the process removed, or EMPTY if queue is empty
 */
unsigned int dequeue(struct list *q)
{
        //get the head entry of the queue
      	struct node* originalHead = q-> head;

        // unlink the head entry from the rest
      	struct node* newHead = originalHead-> next;
      	newHead-> prev = NULL;
      	q-> head = newHead;

        //free up the space on the heap
      	int keyToReturn= originalHead->key;
      	free(originalHead);
        //return the pid on success
        q->size -=1;
        return keyToReturn;
}


/**
 * Remove a process from an arbitrary point in a queue
 * @param pid	ID of process to remove
 * @param q	Pointer to the queue
 * @return pid on success, SYSERR if pid is not found
 */
unsigned int	removeEntry(unsigned int key, struct list *q)
{

	//remove process identified by pid parameter from the queue and return its pid
	struct node* current = q-> head;
	while(current != NULL){
		if(current->key == key){
			//match found, remove from list and return
			struct node* match = current;
			//middle of list
			if((match-> next != NULL) && (match-> prev != NULL)){
				match-> next-> prev = match-> prev;
				match-> prev-> next = match-> next;
			}

			else if(match-> prev == NULL){ //found at head
		    struct node* newHead = match-> next;
		    newHead-> prev = NULL;
		     q-> head = newHead;
			}

			else if(match-> next == NULL){ //found at tail
				struct node* newTail = match->prev;
      	newTail->next = NULL;
      	q-> tail = newTail;
			}

			else{ //list of one
				q-> tail = NULL;
				q-> head = NULL;
			}

			q->size -=1;
			free(match);

			return key;
		}
		current = current->next;
	}
  return -1; //page number does not exist
}
