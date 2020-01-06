//definitions
#define AND_QUERY 1
#define OR_QUERY 0
#define NUM_INDEXES 101 //size of cache -- ie. how many possible indexes
#define LIST_SIZE 1000

//includes
#include "Control.h"
#include "QueryData.h"
#include "hashData.h"

/*
struct hashTable{
  int size;
  struct node **list;
};
*/

// ESSENTIAL HASH FUNCTIONS
struct list **createTable();
void hash_insert(struct list **hashTable, int start, int end, word_32 *data, int column_bits);
unsigned int hash_remove(struct list **hashTable, int index);
void hashmap_destroy(struct list **hashTable);

// LOOKUP FUNCTIONS
unsigned int jarvis_lookup(queryData *ret, struct list **hashTable, int start, int end, unsigned int key, int column_bits);
int hash_lookup(queryData *ret, struct list **hashTable, int real_start, int new_start, int real_end, unsigned int key, int column_bits);

// SORTING FUNCTIONS
// void MergeSort(struct node **headRef);
// struct node* SortedMerge(struct node *a, struct node *b);
// void FrontBackSplit(struct node *source, struct node **head, struct node **tail);


// HELPER FUNCTIONS
int getStartCol(unsigned int key, int column_bits);
int getEndCol(unsigned int key, int column_bits);
int hashCode(int start);
unsigned int hash_key_builder(int start, int end, char *oper, int column_bits);

// PRINTER FUNCTIONS
void printHashList(struct list **hashTable, int index, int column_bits);
void printSubranges(struct node **subranges, int maxSubranges, int column_bits);
void printHashTable(struct list **hashTable, int column_bits);
