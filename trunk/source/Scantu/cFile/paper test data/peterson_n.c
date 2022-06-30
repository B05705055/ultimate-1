//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 3));

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#define N 3
typedef unsigned long int pthread_t;

// int flag1 = 0, flag2 = 0; // boolean flags
// int turn; // integer variable to hold the ID of the thread whose turn is it
int x = 0; // boolean variable to test mutual exclusion

int level[N] = {-1};
int last_to_enter[N-1] = {-1};
  
// void *thr(int i){
void *thr(void* k){
    int i = *((int *) k);
    for(int l = 0; l < N-1; ++l)
    {
        level[i] = l;
        last_to_enter[l] = i;
 
        for(int k = 0;k < N;k++)
        {
            while(k!=i && level[k]>= l && last_to_enter[l]==i)
            {

            };
        }

        // while(last_to_enter[l]==i)
        // {
        //     for(int k = 0;k < N-1;++k)
        //     {
        //         while(k!=i && last_to_enter[k]>= l && last_to_enter[l]==i)
        //         {

        //         };
        //     }
        // };

    }
    // begin: critical section
    x++;
    //printf("%d\n",x);
    // end: critical section
    level[i] = -1;
    return 0;
}
// void *thr1(void *_) {
//     flag1 = 1;
//     turn = 1;
//     int f21 = flag2;
//     int t1 = turn;
//     while (f21==1 && t1==1) {
//         f21 = flag2;
//         t1 = turn;
//     };
//     // begin: critical section
//     x++;
//     // end: critical section
//     flag1 = 0;
//     return 0;
// }

// void *thr2(void *_) {
//     flag2 = 1;
//     turn = 0;
//     int f12 = flag1;
//     int t2 = turn;
//     while (f12==1 && t2==0) {
//         f12 = flag1;
//         t2 = turn;
//     };
//     // begin: critical section
//     x++;
//     // end: critical section
//     flag2 = 0;
//     return 0;
// }
  
int main() {
  // pthread_t *tid = malloc( N * sizeof(pthread_t) );
  // for(int i=0; i<N; i++) 
  // {
  //   pthread_create(&tid[i], NULL, thr,(void *)i);
  // }
  // for(int i=0; i<N; i++)
  // {
  //   pthread_join(tid[i], NULL);
  // }
    int i[3] = {0, 1, 2};

    pthread_t t0, t1, t2;
    pthread_create(&t0, 0, thr, (void*) &i[0]);
    pthread_create(&t1, 0, thr, (void*) &i[1]);
    pthread_create(&t2, 0, thr, (void*) &i[2]);
    // pthread_create(&t3, 0, f, (void*) &i[3]);

    // pthread_t t0, t1, t2;
    // pthread_create(&t0, 0, thr, 0);
    // pthread_create(&t1, 0, thr, 1);
    // pthread_create(&t2, 0, thr, 2);

    pthread_join(t0, 0);
    pthread_join(t1, 0);
    pthread_join(t2, 0);

 //  for(int i=0;i<N;i++) {
 //   pthread_t t;
 //   pthread_create(&t, NULL, (void*) thr, &i); 
 //   pthread_join(t, 0);
 // }

  //printf("%d\n",x);
  return 0;
}