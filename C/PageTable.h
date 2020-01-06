//final variables
#define ADD_LENGTH_32 31
#define ONES_32 0x7FFFFFFF
#define ADD_LENGTH_64 61
#define ONES_64 0x7FFFFFFFFFFFFFFF

//globals
word_32 *pageTableStart; //page table indexed by starting colmn -- only has a valid bit
word_64 **pageTableEnd; //page table indexed by ending column -- has valid bit and data
int *tableExist; //holds a value for each column -- -1 if pageTableEnd doesn't exist yet
int column_bits; //amount of bits needed for a column number
int data_bits; //amount of bits needed for the data

//functions
void print_startTb(int num_columns);
void print_endTb(int startColumn, int num_columns);
void addToCache(int start, int end, int num_columns, queryData *results);
int checkQuery(int start, int end, int num_columns);
word_32 getData(int start, int end);
void init_pagetb(int num_columns);
void freeCache(int num_columns);
