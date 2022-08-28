//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 4));

#include <stdio.h>
#include <stdbool.h>
#include <pthread.h>
#define N 4
//typedef unsigned long int pthread_t;

int x = 0;
int token = 0;
int enter_num = -1;
int flag[N] = {0};
int thr_num_array[N];

void *thr(void* k){
	int thr_num = *((int *) k);
	printf("thr %d start\n", thr_num);
	
	flag[thr_num] = 1;
	while(enter_num != thr_num){
	}
	
	// begin: critical section
	int y = 0;
	y = x;
	y++;
    x = y;
	printf("thr %d exit from critical section, the value of x is: %d now\n", thr_num, x);
	// end: critical section
	flag[thr_num] = 0;
	
	pthread_exit(NULL);
}

void *pass_ring(void* k){
	int thr_num = *((int *) k);
	printf("pr %d start\n", thr_num);
	
	while(true){
		while (token != thr_num){
		}
		
		if (flag[thr_num] == 1){
			enter_num = thr_num;
			while(flag[thr_num] == 1){
			}
			enter_num = -1;
		}
		token = (token + 1) % N;
	}
	
	pthread_exit(NULL);
}

int main(){
	
	for (int i = 0; i < N; i++){
		thr_num_array[i] = i;
	}
	
	pthread_t tt[N];
	pthread_t pr[N];
	
	for (int i = 0; i < N; i++){
		pthread_create(&tt[i], NULL, thr, &thr_num_array[i]);
		pthread_create(&pr[i], NULL, pass_ring, &thr_num_array[i]);
	}
	
	for (int i = 0; i < N; i++){
		pthread_join(tt[i], NULL);
		pthread_join(pr[i], NULL);
	}
	
	return 0;
}