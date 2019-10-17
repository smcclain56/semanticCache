#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "Hashmap.h"
#include "WAHQuery.h"
#include "Queue.h"

/*
Author @smcclain
*/

/*
Creates the hash table from size given
@return
  returns struct list **hashTable that was initialized
*/
struct list **createTable(){
  //malloc table
  struct list **hashTable = (struct list**) malloc(sizeof(struct list*) * NUM_INDEXES);
  // column_bits = (int) ceil(log10(num_columns) / log10(2));
  //malloc each list and initalize
  int i;
  for(i=0; i<NUM_INDEXES; i++){
    hashTable[i] = (struct list*) malloc(sizeof(struct list));
    hashTable[i]->head = NULL;
    hashTable[i]->tail = NULL;
    hashTable[i]->size = 0;
  }
  return hashTable;
}

/*
Hash function -- converts a unique key to a hashcode that is used
as an index into the hash table
@param
  unsigned int key -> unique key --> [AND(1)/OR(0)STARTEND]
@return
  int hashCode -> index for hash table
*/
int hashCode(int start){
  int ret;
  int prime = 17;
  ret = prime + start;
  // ret = (prime * start)*(pow(start,2));
  return ret;
}

/*
Gets the start column from the key to use in hashCode
@param
  unsigned int key --> unique key of data
@return
  int start key --> starting column of key inputted as param
*/
int getStartCol(unsigned int key, int column_bits){
  //get start column from key
  unsigned int bitMask;
  int start_key;
  //get rid of most sig bit
  key = key << 1;
  key = key >> 1;
  bitMask = BITMASK >> (BASE_LEN-column_bits);
  start_key = (key & bitMask);
  //return
  return start_key;
}

/*
Gets the end column from the ke
@param
  unsigned int key --> unique key of data
@return
  int end key --> ending column of key inputted as param
*/
int getEndCol(unsigned int key, int column_bits){
  //get end column from key
  unsigned int bitMask;
  int end_key;
  //get rid of most sig bit
  key = key << 1;
  key = key >> 1;
  end_key = key >> column_bits;
  bitMask = BITMASK >> (BASE_LEN-column_bits);
  end_key = (end_key & bitMask);
  //return
  return end_key;
}

/*
Adds a node to the hash table
@param
  struct hashTable *t -> hash table
  unsigned int key -> unique key of node being added
  word_32 val -> value (data) of node being added
*/
void hash_insert(struct list **hashTable, int start, int end, word_32 *data, int column_bits)
{
  //printf("insert [%d,%d] \n",start,end );
  //malloc new node and get list of hashTable
  unsigned int key = hash_key_builder(start, end, "OR", column_bits);
  int index = abs(hashCode(start))%NUM_INDEXES;
  struct list *list = hashTable[index];
  struct node *newNode = (struct node*) malloc(sizeof(struct node));
  newNode->next = NULL;
  newNode->prev = NULL;
  newNode->key = key; //DC - this is okay?
  newNode->val = data; //DC - this is not.
  //newNode->val = &TESTING;

  //insert new node when list is empty
  if(list->head == NULL){
    //first node added
    list->head = newNode;
    list->tail = newNode;
    newNode->next = NULL;
    newNode->prev = NULL;
  }
  else {
    if(LOOKUP == JARVIS){ //jarvis requires lists to be sorted by end column
      int head_end = getEndCol(list->head->key, column_bits);
      int tail_end = getEndCol(list->tail->key,column_bits);

      if(end > head_end){ //end column of new entry is less than head
        //printHashList(hashTable,index, column_bits);
        newNode->next = list->head;
        list->head->prev = newNode;
        list->head = newNode;
      }else if(end < tail_end){ // end column is larger than tail
        list->tail->next = newNode;
        newNode->prev = list->tail;
        list->tail = newNode;
      }else{ //end column goes somewhere in the middle
        struct node *current = list->head;
        while(current->next != NULL && end < getEndCol(current->next->key,column_bits)){
          current = current->next;
        }
        newNode->prev = current;
        newNode->next = current->next;
        current->next = newNode;
        current->next->prev = newNode;
        }
      }else{
        //list is not empty to add onto tail of queue when doing find-split lookup
        list->tail->next = newNode;
        newNode->prev = list->tail;
        newNode->next = NULL;
        list->tail = newNode;
      }
    }
  list->size++;

}


/*
Looks up a node in the hash table -- recursive and using OR_WAH
to combine results vector of query added to data already in results vector
@param
  struct HashTable *t -> hash table
  unsigned int key-> unique key of node to look up
@return
  end column of last results vector found
*/
int hash_lookup(queryData *ret, struct list **hashTable, int real_start, int new_start, int real_end, unsigned int key, int column_bits){
  if(real_start != new_start){
    ret->complete_cache = 0;
  }
  //set current to head of list at given index
  int index = abs(hashCode(new_start))%NUM_INDEXES;
  struct list *list = hashTable[index];
  //int tempindex;

  //if list is empty return right away
  if(list->head == NULL) {
    //printf("\treturn right away -- list is emtpty\n" );
    //printHashTable(hashTable,column_bits);

    return new_start-1;
  }

  //printf("set current\n" );
  //set up to hold onto node most similar to wanted key
  struct node *current = list->head;
  struct node *mostComparable = list->head;
  unsigned int partial_key = mostComparable->key;
  int partial_start = getStartCol(partial_key, column_bits);
  int partial_end = getEndCol(partial_key, column_bits);
  int best_diff = ((new_start - partial_start)+(real_end - partial_end));
  int current_diff;

   //printf("LOOKUP START: indexed in=%d\n", index);
   //printf("real_start=%d, new_start=%d, real_end=%d\n", real_start, new_start, real_end);
   //printHashTable(hashTable, column_bits);
   //printf("LOOKUP END\n");


  //printf("while loop\n");
  while (current != NULL){
    if(current->key == key){ //complete query found //TODO THIS LINE IS CAUSING AN ERROR IN LOOKUP
      if(real_start == new_start){
        ret->result = current->val; //was first query searched for then we found full query
      }else{
        //otherwise OR new piece to old pieces found to make full query
        OR_WAH(ret->result, ret->result, ret->resultSize, current->val, ret->resultSize);
      }
      //printf("return full query\n" );
      return real_end; //return full query
    }
    //if not found then see if it's the most comparable
    partial_start = getStartCol(partial_key, column_bits);
    partial_end = getEndCol(partial_key, column_bits);
    current_diff = ((new_start - partial_start)+(real_end - partial_end));
    if(abs(current_diff) < abs(best_diff)){ //if it is most comparble then update mostComparable pointer
      mostComparable = current;
      partial_key = mostComparable->key;
      best_diff = current_diff;
    }

    current = current->next; //move to next node
  }
  //not in list -- recurse from most comparable query found
  partial_key = mostComparable->key;
  partial_start = getStartCol(partial_key, column_bits);
  partial_end = getEndCol(partial_key, column_bits);
  //printf("Exact match not found. Best results: start=%d, end=%d\n",partial_start,partial_end );
  if(partial_end >= real_end){ //if we already have more columns than needed then no need to recurse
    //printf("returning partial results [%d,%d]\n",real_start,partial_end );
    if(real_start == new_start){ //if first query found then return its value
      ret->result = mostComparable->val;
    }else{ //otherwise OR it to what we already have
      OR_WAH(ret->result, ret->result, ret->resultSize, current->val, ret->resultSize);
    }
    return partial_end; //return what column we ended on
  }else{
    //printf("recursing [%d,%d]\n",partial_end+1, real_end); //recurse on column we ended on
    unsigned int new_key = hash_key_builder(partial_end+1, real_end, "OR", column_bits); //TODO CHANGE TO BE AND/OR
    return hash_lookup(ret, hashTable, real_start, partial_end+1,real_end, new_key, column_bits);
  }
}

void printSubranges(struct node **subranges, int numRanges, int column_bits){
  int i;
  for(i=0; i<numRanges; i++){
    if(subranges[i] != NULL){
      printf("#%d   start = %d, end = %d \n",i,getStartCol(subranges[i]->key,column_bits), getEndCol(subranges[i]->key, column_bits) );
    }
  }
  printf("\n" );
}

unsigned int jarvis_lookup(queryData *ret, struct list **hashTable, int start, int end, unsigned int key, int column_bits){
  // initalize current_start and current_end
  int current_start = INFINITY;
  int current_end = -INFINITY;

  // find list at start index
  int maxSubranges = end-start + 1;
  struct node **subranges = (struct node**) malloc (sizeof(struct node*)*maxSubranges);
  struct list *list;
  struct node *current;
  int index;
  unsigned int found_key;

  int currentRange = 0;
  int k = end-start;
  int i = start;

  // traverse each list in range for subranges
  // printf("\tBEGIN SEARCH FOR SUBRANGES\n" );
  //printHashTable(hashTable,column_bits);

  // SORT IN ASCENDING ORDER //is it already sorted? sort each list from start to end-1 before finding subranges

  for(i=start; i <= start + k; i++ ){
    index = abs(hashCode(i))%NUM_INDEXES;
    list = hashTable[index];
    //printf("CHECKING INDEX: %d\n",index );
    //printf("------ BEFORE SORTING --------\n" );
    //printHashList(hashTable,i,column_bits);
    //MergeSort(&list->head);
    //printf("------ AFTER SORTING --------\n" );
    //printHashList(hashTable,i,column_bits);
    if(list->head == NULL){
      // printf("list is empty\n");
      continue;
    }

    current = list->head;
    // loop through list until and end > current_end is found
    while(current != NULL){
      if(getEndCol(current->key,column_bits) > end){ //removes subranges with too much coverage
        current = current->next;
        continue;
      }
      if(current_end < getEndCol(current->key, column_bits)){
        if(current_start == INFINITY){ //update current_start only on the first instance
          current_start = i;
        }
        current_end = getEndCol(current->key,column_bits);
        subranges[currentRange] = current;
        currentRange++;
        break;
      }
      current = current->next;
    }
  }
  int totalSubRanges = currentRange;
  // printf("\tEND FIND SUBRANGES\n" );

  // if no subranges were found then return empty set
  if(subranges[0] == NULL){
    found_key = hash_key_builder(start,start-1,"OR", column_bits);
    return found_key;
  }
  // firt subrange must be included because it provides unique coverage
  struct node *temp = subranges[0];
  ret->result = temp->val;
  int ret_start = getStartCol(temp->key,column_bits);
  current_end = getEndCol(temp->key,column_bits);

  // if full query was cached then return right away
  if(ret_start == start && current_end == end){
    found_key = hash_key_builder(start,end,"OR", column_bits);
    return found_key;
  }

  // otherwise find subranges with most coverage
  i=1;
  int found;
  //printSubranges(subranges,totalSubRanges,column_bits);
  while(i<totalSubRanges){
    found = 0;
    while(i < totalSubRanges && getStartCol(subranges[i]->key,column_bits) <= current_end + 1){
      i++;
      found =1;
    }
    if(found == 1){ //if we entered loop back up one spot
      i--;
    }
    OR_WAH(ret->result, ret->result, ret->resultSize, subranges[i]->val, ret->resultSize);
    current_end = getEndCol(subranges[i]->key, column_bits);
    i++;
  }
  // if full query was formed during jarvis, it must be inserted into the cache still
  if(current_end == end){
    ret->complete_cache = 0;
  }

  // return
  found_key = hash_key_builder(ret_start, current_end, "OR", column_bits);
  return found_key;
}

// void MergeSort(struct node **headRef)
// {
//   struct node *head = *headRef;
//   struct node *a;
//   struct node *b;
//   if ((head == NULL) || (head->next == NULL)){
//      return;
//   }
//   FrontBackSplit(head, &a, &b);
//   MergeSort(&a);
//   MergeSort(&b);
//   *headRef = SortedMerge(a, b);
// }
//
// struct node* SortedMerge(struct node *a, struct node *b)
// {
//   struct node* result = NULL;
//   if (a == NULL){
//     return(b);
//   }else if (b == NULL){
//       return(a);
//   }
//   if (a->val <= b->val){
//     result = a;
//     result->next = SortedMerge(a->next, b);
//   }else{
//     result = b;
//     result->next = SortedMerge(a, b->next);
//   }
//   return(result);
// }
//
// void FrontBackSplit(struct node *source, struct node **head, struct node **tail)
// {
//   struct node *fast;
//   struct node *slow;
//   slow = source;
//   fast = source->next;
//   while (fast != NULL){
//     fast = fast->next;
//     if (fast != NULL){
//       slow = slow->next;
//       fast = fast->next;
//     }
//   }
//   *head = source;
//   *tail = slow->next;
//   slow->next = NULL;
// }


/*
Prints the linked list a specific index of the hash table
@param
  struct list **hashTable --> hashTable containing the linked lists
  int index --> index of hashTable you want to print list of
*/
void printHashList(struct list **hashTable, int index, int column_bits){
  printf("\n" );
  struct list *list = hashTable[index];
  struct node *current = list->head;
  int start,end;
  int i=0;
  if(current == NULL){
    printf("list is empty\n" );
  }
  while(current != NULL){
    start = getStartCol(current->key, column_bits);
    end= getEndCol(current->key, column_bits);
    printf("QueueEntry %d: start = %d, end =%d, next=%p\n", i, start, end, current->next);
    current = current->next;
    i++;
  }
  printf("\n" );

}

/*
Prints the entire hash table -- all lists
@parm
  struct list **hashTable --> table to be printed
*/
void printHashTable(struct list **hashTable, int column_bits){
  int i,j;
  struct list *list;
  struct node *current;
  int start_key;
  int end_key;
  for(i=0; i<NUM_INDEXES; i++){
    j=0;
    list = hashTable[i];
    current = list->head;
    if(list->size == 0){
      continue;
    }
    printf("Index #%d size=%d\n", i,list->size);
    while(current != NULL){
      start_key = getStartCol(current->key, column_bits);
      end_key = getEndCol(current->key, column_bits);
      printf("\t{#%d: key = %.8X start = %d end = %d next=%p} \n", j, current->key, start_key, end_key, current->next);
      current = current->next;
      j++;
    }
  }
    printf("\n###################################\n" );
}

/*
Creates the string to be used as the key for the hash table
@param
  char *key -> string to be added to to create key
  int start -> starting column of query in hash map
  int end -> ending column of query in hash map
  char *oper -> string representing AND/OR depending on type of query in hash map
@return
  key string is returned in char* key param
  function is void
*/
unsigned int hash_key_builder(int start, int end, char *oper, int column_bits){
  unsigned int ret;
  unsigned int temp;
  if(strcmp(oper,"OR")==0){ //"OR"
    ret = 0x00000000;
  }else{ //"AND"
    ret = 0x80000000;
  }
  //put end before start

  temp = end << (column_bits);
  //concatenate end and start
  ret = ret | temp | start;
  return ret;
}

/*
Removes the head of list at index specified and returns it's key value
@param
  struct list **hashTable --> hash table to remove element from
  int index --> which list from hash table to remove from
@return
  unsigned int key of node removed
*/
unsigned int hash_remove(struct list **hashTable, int index){
  //get the head entry of the list
  struct list *list = hashTable[index];
  struct node *originalHead = list->head;

  //unlink the head entry from the rest
  struct node *newHead = originalHead->next;
  newHead->prev = NULL;
  list->head = newHead;

  //free up space on the heap
  unsigned int deleted_key = originalHead->key;
  free(originalHead);
  list->size -= 1;

  //return deleted key on success
  return deleted_key;
}

void hashmap_destroy(struct list **hashTable)
{
  int i,j;
  //int start, end;
  struct list *list;
  struct node *current;
  struct node *next;
  for(i=1; i<NUM_INDEXES; i++){ //TODO FOR SOME REASON DOESN'T WORK FOR INDEX 0 BUT WORKS FOR ALL OTHERS
    list = hashTable[i];
    current = list->head;
    j=0;
    while(current != NULL){
      next = current->next;
      free(current); //TODO why is this not working again
      // current = NULL;
      current = next;
      j++;
    }
    free(list);
  }
  free(hashTable);
}
