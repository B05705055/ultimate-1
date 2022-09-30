// ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 2));
//@ ltl invariant positive: <>[](AP(x == 1) || AP(x == 2));

/* Testcase from Threader's distribution. For details see:
   http://www.model.in.tum.de/~popeea/research/threader
*/

#include <pthread.h>
typedef unsigned long int pthread_t;

int x = 0; // boolean variable to test mutual exclusion

void *thr1(void *_) {
    // begin: critical section
    // x = 0;
	int y1 = 0;
	y1 = x;
	y1++;
    x = y1;
    // end: critical section
    return 0;
}

void *thr2(void *_) {
    // begin: critical section
    // x = 1;
    int y2 = 0;
	y2 = x;
	y2++;
    x = y2;
    // end: critical section
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