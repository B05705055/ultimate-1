//#Unsafe
/*
 * This is an example for a non-obvious bug in a C program.
 * Assume that array arr is some cyclic buffer of size 256.
 * In each iteration we write two new values in the buffer.
 * Since the current position pos is an unsigned char one might expect that
 * (pos + 1) is always between 0 and 255. However, because of the "usual
 * arithmetic conversions" the expression pos + 1 has type int and can be
 * evaluated to 256.
 * 
 * Date: 2016-02-11
 * Author: heizmann@informatik.uni-freiburg.de
 * 
 */
#include <stdlib.h>

int main() {
    int aaa[2];
    aaa[0] = 5;aaa[1] = 3;
    int x = aaa[0];
    int aaa[2];
	
	for (int i = 0; i < 10; i++){
		fadsgaeg;
	}
	
	for (int i /*
		commets test 1
	*/){uncomment /*commets test 2*/ part}
	
	for (int i = 0; i < 10; i++){000000; 	}
	
	for (int i = "{";)  	{11111;}
	
	if(int i = "{;)  	{22222;}
	fsdgsdhshs") test no block if;
	
	for(int i = "{;)  	{22222;}
	fsdgsdhshs") test no block for;
	
	if(int i = "{\";")  	{33333;}

    return 0;
}