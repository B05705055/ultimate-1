//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 4));

/* Testcase from Threader's distribution. For details see:
   http://www.model.in.tum.de/~popeea/research/threader
*/
// #include <stdio.h>
#include <pthread.h>
typedef unsigned long int pthread_t;

int flag1 = 0, flag2 = 0; // boolean flags
int turn; // integer variable to hold the ID of the thread whose turn is it
int x = 0; // boolean variable to test mutual exclusion
int a = 0;
int b = 0;

void *thr1(void *_) {
    start1:
        a++;
        flag1 = 1;
        turn = 1;
        int f21 = flag2;
        int t1 = turn;
        while (f21==1 && t1==1) {
            f21 = flag2;
            t1 = turn;
        };
        // begin: critical section
        x++;
        // end: critical section
        flag1 = 0;
        // printf("%d\n",x);
        if(a!=2)
        {
            goto start1;
        }
    return 0;
}


void *thr2(void *_) {
    start2:
        b++;
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
        // printf("%d\n",x);
        // end: critical section
        flag2 = 0;
        if(b!=2)
        {
            goto start2;
        }
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