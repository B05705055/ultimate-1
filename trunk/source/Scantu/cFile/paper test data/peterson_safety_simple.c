//@ ltl invariant positive: []!(AP(x == 1) && AP(y == 1));

/* Testcase from Threader's distribution. For details see:
   http://www.model.in.tum.de/~popeea/research/threader
   Unsafe example
*/
#include <pthread.h>
typedef unsigned long int pthread_t;

int x = 0; // boolean variable to test mutual exclusion
int y = 0;

void *thr1(void *_) {
    x++;
    x--;
    // int b = x;
    // b++;
    // x = b;
    // int a = x;
    // a--;
    // x = a;
    return 0;
}

void *thr2(void *_) {
    y++;
    y--;
    // int c = y;
    // c++;
    // y = c;
    // int d = y;
    // d--;
    // y = d;
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