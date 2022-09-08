//@ ltl invariant positive: ( U AP(fairness_label_end == (N-1))) ==> <>[]AP(x == N);

#include <stdio.h>
#include <pthread.h>
#define N 4
typedef unsigned long int pthread_t;

int x = 0;

int level[N] = {-1};
int last_to_enter[N-1] = {-1};

int thr_num_array[N];

int fairness_label = 0;
int fairness_label_end = 0;

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
				fairness_label = i;
            };
        }

    }
    // begin: critical section
    x++;
    //printf("%d\n",x);
    // end: critical section
    level[i] = -1;
	
    fairness_label_end++;
	
	pthread_exit(NULL);
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