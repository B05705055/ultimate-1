//@ ltl invariant positive: []((AP(flag1 == 0) || AP(flag2 == 0)) ==> <>AP(x == 2));

/* Testcase from Threader's distribution. For details see:
   http://www.model.in.tum.de/~popeea/research/threader
*/

#include <pthread.h>
typedef unsigned long int pthread_t;

int flag1 = 1, flag2 = 1; // boolean flags
int turn; // integer variable to hold the ID of the thread whose turn is it
int x = 0; // boolean variable to test mutual exclusion

void *thr1(void *_) {
    turn = 1;
    int f21 = flag2;
    int t1 = turn;
    while (f21==1 && t1==1) {
        f21 = flag2;
        t1 = turn;
    };
    // begin: critical section
    // x = 0;
	int y1 = 0;
	y1 = x;
	y1++;
    x = y1;
    // end: critical section
    flag1 = 0;
    return 0;
}

void *thr2(void *_) {
    turn = 0;
    int f12 = flag1;
    int t2 = turn;
    while (f12==1 && t2==0) {
        f12 = flag1;
        t2 = turn;
    };
    // begin: critical section
    // x = 1;
    int y2 = 0;
	y2 = x;
	y2++;
    x = y2;
    // end: critical section
    flag2 = 0;
    return 0;
}
  
int main() {
  pthread_t t1, t2;
  pthread_create(&t1, 0, thr1, 0);
  pthread_create(&t2, 0, thr2, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  return 0;
}