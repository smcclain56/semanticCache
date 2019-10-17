# A note to aingerson, por que no Makefile?
#all :
#	gcc -g -Wall -c ActiveWord.c
#	gcc -g -Wall -c Clock.c
#	gcc -g -Wall -c Query.c
#	gcc -g -Wall -c QueryUtil.c
#	gcc -g -Wall -c RawBitmapReader.c
#	gcc -g -Wall -c SegUtil.c
#	gcc -g -Wall -c VALCompressor.c
#	gcc -g -Wall -c VALQuery.c
#	gcc -g -Wall -c WAHCompressor.c
#	gcc -g -Wall -c Writer.c
#	gcc -g -Wall -o main main.c ActiveWord.o Clock.o Query.o QueryUtil.o RawBitmapReader.o SegUtil.o VALCompressor.o VALQuery.o WAHCompressor.o WAHQuery.o Writer.o -lpthread -lm
#clean:
#	rm main
#	rm *.o

all : main.c ActiveWord.o BlockSeg.h Clock.o Control.h Core.h hashData.h Query.o QueryData.h QueryUtil.o RawBitmapReader.o SegUtil.o VALCompressor.o VALQuery.o Vars.h WAHCompressor.o WAHQuery.o Writer.o PageTable.o Hashmap.o Queue.o
		gcc -O0 -Wall -o main main.c ActiveWord.o Clock.o Query.o QueryUtil.o RawBitmapReader.o SegUtil.o VALCompressor.o VALQuery.o WAHCompressor.o WAHQuery.o Writer.o PageTable.o Hashmap.o Queue.o -lpthread -lm

ActiveWord.o : ActiveWord.h ActiveWord.c
		gcc -O0 -Wall -c ActiveWord.c

Clock.o : Clock.h Clock.c
		gcc -O0 -Wall -c Clock.c

Query.o : Query.h Query.c
		gcc -O0 -Wall -c Query.c

QueryUtil.o : QueryUtil.h QueryUtil.c
		gcc -O0 -Wall -c QueryUtil.c

RawBitmapReader.o : RawBitmapReader.h RawBitmapReader.c
		gcc -O0 -Wall -c RawBitmapReader.c

SegUtil.o : SegUtil.h SegUtil.c
		gcc -O0 -Wall -c SegUtil.c

VALCompressor.o : VALCompressor.h VALCompressor.c
		gcc -O0 -Wall -c VALCompressor.c

VALQuery.o : VALQuery.h VALQuery.c
		gcc -O0 -Wall -c VALQuery.c

WAHCompressor.o : WAHCompressor.h WAHCompressor.c
		gcc -O0 -Wall -c WAHCompressor.c

WAHQuery.o : WAHQuery.h WAHQuery.c
		gcc -O0 -Wall -c WAHQuery.c

Writer.o : Writer.h Writer.c
		gcc -O0 -Wall -c Writer.c

PageTable.o: PageTable.h PageTable.c
		gcc -O0 -Wall -c PageTable.c

Hashmap.o: Hashmap.h Hashmap.c
		gcc -O0 -Wall -c Hashmap.c

Queue.o: Queue.h Queue.c
		gcc -O0 -Wall -c Queue.c

clean:
		rm -f main *.o
