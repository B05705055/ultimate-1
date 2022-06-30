//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 2));

/* Testcase from Threader's distribution. For details see:
   http://www.model.in.tum.de/~popeea/research/threader
   The same while loop condition leads to thr1 and thr2 enter 
   critical section at the same time, and the value of x is 1.
   [Thread0@L56, Thread1@L27-1, Thread2@L45]accept_S2

   thr1 and thr2 in while loop indefinitely, since turn = 1 -> turn = 0 (f1=1,f2=1)
*/

#include <pthread.h>
typedef unsigned long int pthread_t;

int flag1 = 0, flag2 = 0; // boolean flags
int turn; // integer variable to hold the ID of the thread whose turn is it
int x = 0; // boolean variable to test mutual exclusion

void *thr1(void *_) {
    flag1 = 1;
    turn = 1;
    int f21 = flag1;
    int t1 = turn;
    while (f21==1 && t1==0) {
        f21 = flag1;
        t1 = turn;
    };
    // begin: critical section
    x++;
    // end: critical section
    flag1 = 0;
    return 0;
}

void *thr2(void *_) {
    flag2 = 1;
    turn = 0;
    int f12 = flag1;
    int t2 = turn;
    while (f12==1 && t2==0) {
        f12 = flag1;
        t2 = turn;
    };
    // begin: critical section
    x++;
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