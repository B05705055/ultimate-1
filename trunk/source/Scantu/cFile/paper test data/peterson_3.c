//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 1));

/* Testcase from Threader's distribution. For details see:
   http://www.model.in.tum.de/~popeea/research/threader
*/

#include <pthread.h>
typedef unsigned long int pthread_t;

int flag0 = 0, flag1 = 0, flag2 = 0, flag3 = 0; // boolean flags
int turn; // integer variable to hold the ID of the thread whose turn is it
int x = 0; // boolean variable to test mutual exclusion

void *thr0(void *_) {
    flag0 = 1;
    if(turn == 1)
    {
      turn = 1;
      // int f21 = flag2;
      // int t1 = turn;
      while (flag1==1 && turn==1) {
          // f21 = flag2;
          // t1 = turn;
      };
    }
    if(turn == 2)
    {
      turn =2;
      while(flag2==1 && turn==2)
      {

      };
    }
    if(turn == 3)
    {
      turn = 3;
      while(flag3==1 && turn==3)
      {

      };
    }
    // begin: critical section
    x++;
    // end: critical section
    flag0 = 0;
    return 0;
}

void *thr1(void *_) {
    flag1 = 1;
    
    if(turn == 0)
    {
      turn = 0;
      // int f12 = flag1;
      // int t2 = turn;
      while (flag0==1 && turn==0) {
          // f12 = flag1;
          // t2 = turn;
      };
    }
    if(turn == 2)
    {
      turn =2 ;
      while(flag2==1 && turn==2)
      {

      };
    }
    if(turn == 3)
    {
      turn = 3;
      while(flag3==1 && turn==3)
      {

      };
    }
    // begin: critical section
    x++;
    // end: critical section
    flag1 = 0;
    return 0;
}
  
void *thr2(void *_) {
    flag2 = 1;
    
    if(turn == 0)
    {
      turn = 0;
      // int f12 = flag1;
      // int t2 = turn;
      while (flag0==1 && turn==0) {
          // f12 = flag1;
          // t2 = turn;
      };
    }
    if(turn == 1)
    {
      turn = 1 ;
      while(flag1==1 && turn==1)
      {

      };
    }
    if(turn == 3)
    {
      turn = 3;
      while(flag3==1 && turn==3)
      {

      };
    }
    // begin: critical section
    x++;
    // end: critical section
    flag2 = 0;
    return 0;
}

void *thr3(void *_) {
    flag3 = 1;
    
    if(turn == 0)
    {
      turn = 0;
      // int f12 = flag1;
      // int t2 = turn;
      while (flag0==1 && turn==0) {
          // f12 = flag1;
          // t2 = turn;
      };
    }
    if(turn == 1)
    {
      turn = 1 ;
      while(flag1==1 && turn==1)
      {

      };
    }
    if(turn == 2)
    {
      turn = 2;
      while(flag2==1 && turn==2)
      {

      };
    }
    // begin: critical section
    x++;
    // end: critical section
    flag3 = 0;
    return 0;
}


int main() {
  pthread_t t0, t1, t2, t3;
  // pthread_t t0, t1;
  pthread_create(&t0, 0, thr0, 0);
  pthread_create(&t1, 0, thr1, 0);
  pthread_create(&t2, 0, thr2, 0);
  pthread_create(&t3, 0, thr3, 0);
  pthread_join(t0, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  pthread_join(t3, 0);
  return 0;
}