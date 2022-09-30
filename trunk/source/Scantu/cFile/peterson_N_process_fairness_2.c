// ltl invariant positive: <>[]AP(x == 2);
//@ ltl invariant positive: (!(AP(fairness_label_verified == fairness_label_num) ==> []AP(fairness_label_verified == fairness_label_num)) U AP(fairness_label_end == 1)) ==> <>[]AP(x == 2);
// ltl invariant positive: [](AP(fairness_label_end == 1) ==> <>[]AP(x == 2));

#include <stdio.h>
#include <pthread.h>
#define N 2
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
    int y = 0;
	y = x;
	y++;
    x = y;
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
	pthread_t t0, t1;
	
	pthread_create(&t0, NULL, thr, &thr_num_array[0]);
	pthread_create(&t1, NULL, thr, &thr_num_array[1]);
	
	pthread_join(t0, NULL);
	pthread_join(t1, NULL);
	
    return 0;
}