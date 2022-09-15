// ltl invariant positive: <>[]AP(x == 4);
//@ ltl invariant positive: (!(AP(fairness_label_verified == fairness_label_num) ==> []AP(fairness_label_verified == fairness_label_num)) U AP(fairness_label_end == 3)) ==> <>[]AP(x == 3);

#include <stdio.h>
#include <pthread.h>
#define N 4
typedef unsigned long int pthread_t;

int x = 0;

int level[N] = {-1};
int last_to_enter[N-1] = {-1};

int thr_num_array[N];

int fairness_label_num = 0;
int fairness_label_verified = 0;
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
				fairness_label_num = i;
				fairness_label_verified = fairness_label_num;
            }
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
	pthread_t t0, t1, t2, t3;
	
	pthread_create(&t0, NULL, thr, &thr_num_array[0]);
	pthread_create(&t1, NULL, thr, &thr_num_array[1]);
	pthread_create(&t2, NULL, thr, &thr_num_array[2]);
	pthread_create(&t3, NULL, thr, &thr_num_array[3]);
	
	pthread_join(t0, NULL);
	pthread_join(t1, NULL);
	pthread_join(t2, NULL);
	pthread_join(t3, NULL);
	
    return 0;
}