//@ ltl invariant positive: <>[]AP(x == 2);

/* Testcase from Threader's distribution. For details see:
   http://www.model.in.tum.de/~popeea/research/threader
*/

#include <pthread.h>
#define N 2
typedef unsigned long int pthread_t;

int flag0 = 0, flag1 = 0; // boolean flags
int turn; // integer variable to hold the ID of the thread whose turn is it
int x = 0; // boolean variable to test mutual exclusion

void *thr0(void *_) {
    flag0 = 1;
    turn = 1;
    while (flag1==1 && turn==1) {
    };
	
    // begin: critical section
    // x = 0;
	int y0 = 0;
	y0 = x;
	y0++;
    x = y0;
    // end: critical section
    flag0 = 0;
    pthread_exit(NULL);
}

void *thr1(void *_) {
    flag1 = 1;
    turn = 0;
    while (flag0==1 && turn==0) {
    };
	
    // begin: critical section
    // x = 1;
    int y1 = 0;
	y1 = x;
	y1++;
    x = y1;
    // end: critical section
    flag1 = 0;
    pthread_exit(NULL);
}
  
int main() {
  pthread_t t0, t1;
  pthread_create(&t0, 0, thr0, 0);
  pthread_create(&t1, 0, thr1, 0);
  pthread_join(t0, 0);
  pthread_join(t1, 0);
  return 0;
}