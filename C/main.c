#include "Core.h"

#include "RawBitmapReader.h"
#include "Query.h"
#include "Writer.h"
#include "Clock.h"
#include "PageTable.h"
#include "Hashmap.h"
/**
 * Runs formatter/compressor/query engines as set in Control.h
 *
 * format:	F	BITMAP_FILE
 * compress:	C 	BITMAP_FILE		NUM_THREADS		STRIPED/UNSTRIPED
 * query: 	Q 	BITMAP_PATH		QUERY_FILE
 *
 */
int main(int argc, char*argv[]) {
	setbuf(stdout,NULL);

	if(argc>2 && (strcmp(argv[1],"F")==0 || strcmp(argv[1],"C")==0 || strcmp(argv[1],"Q")==0)){
		// printf("correct num of args \n");
		if(argc==3 && strcmp(argv[1],"F")==0){//FORMATTING
			printf("formatting recognized\n");
			if(reformat(&argv[2])==0){
				printf("Unsuccessful reformatting of %s\n",argv[2]);
			}
		}
		// ./main C BITMAP_FILE NUM_THREADS STRIPED/UNSTRIPED
		else if(strcmp(argv[1],"C")==0 && argc==5){//COMPRESSION
			int n = atoi(argv[3]);//number of threads
			if(n<1) return -1;

			if(strcmp(argv[4],"STRIPED")!=0 && strcmp(argv[4],"UNSTRIPED")!=0) return -1;

			char results_name[BUFF_SIZE];
			snprintf(results_name,BUFF_SIZE,"%s_RESULTS.csv",argv[2]);//where the results are being stored

			double time;
			//run compression here
			if(strcmp(argv[4],"UNSTRIPED")==0){
				 time=compress(argv[2], UNSTRIPED,WAH,n);
			}
			else{
				time=compress(argv[2], STRIPED,WAH,n);
			}
			printf("time: %f...\n",time);
			FILE *results_file = fopen(results_name,"a");//open result file (appending to end)
			if(results_file==NULL){
				printf("Failed to open results file %s\n",results_name);
				return 0;
			}
			fprintf(results_file,"%f,",time);//write result to file

			fclose(results_file);
		}
		// ./main   Q 	BITMAP_PATH		QUERY_FILE
		else if(strcmp(argv[1],"Q")==0 && argc==4){
			double clkbegin, clkend, time;
			clkbegin = rtclock();
			//printf("query\n" );
			runQueries(argv[2], argv[3]);
			//printf("query ran\n" );
			clkend = rtclock();
			time = clkend-clkbegin;
			printf("time: %f...\n",time );
		}
	}


	return 0;
}
