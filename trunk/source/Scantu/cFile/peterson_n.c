//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 3));

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#define N 3
typedef unsigned long int pthread_t;

int x = 0;

int level[N] = {-1};
int last_to_enter[N-1] = {-1};

int thr_num_array[N];

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

    }
    // begin: critical section
    x++;
    printf("%d\n",x);
    // end: critical section
    level[i] = -1;
    return 0;
}
  
int main() {
    for (int i = 0; i < N; i++){
		thr_num_array[i] = i;
	}
	
	pthread_t tt[N];
	
	for (int i = 0; i < N; i++){
		pthread_create(&tt[i], NULL, thr, &thr_num_array[i]);
	}
	
	for (int i = 0; i < N; i++){
		pthread_join(tt[i], NULL);
	}
	
    return 0;
}