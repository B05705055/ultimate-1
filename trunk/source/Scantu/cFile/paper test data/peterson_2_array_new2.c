//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 2));

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#define N 2
typedef unsigned long int pthread_t;

int x = 0; // boolean variable to test mutual exclusion

int level[N] = {-1};
int last_to_enter[N-1] = {-1};
  
void *thr(void* k){
    int i = *((int *) k);

    for(int l = 0; l < N-1; ++l)
    {
        level[i] = l;
        last_to_enter[l] = i;
 
        while(last_to_enter[l]==i)
        {
          for(int k = 0;k < N;k++)
          {
            while(k!=i && level[k]>= l){}
          }
          break;
        }
        
    }
    // begin: critical section
    x++;
    //printf("%d\n",x);
    // end: critical section
    level[i] = -1;
    return 0;
}

  
int main() {
    int i[2] = {0, 1};

    pthread_t t0, t1;
    pthread_create(&t0, 0, thr, (void*) &i[0]);
    pthread_create(&t1, 0, thr, (void*) &i[1]);

    pthread_join(t0, 0);
    pthread_join(t1, 0);
    return 0;
}