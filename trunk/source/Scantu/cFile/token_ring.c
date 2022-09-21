// ltl invariant positive: (!(AP(flv == fln) ==> []AP(flv == fln)) U AP(fairness_label_end == 2)) ==> <>[]AP(x == 2);

//@ ltl invariant positive: ((AP(flv == fln) ==> <>AP(fln == ((flv+1)%2))) && (AP(flv_ps == fln_ps) ==> <>AP(fln_ps == ((flv_ps+1)%2)))) ==> <>[]AP(x == 2);

// ltl invariant positive: <>[]AP(x == 2);

#include <stdio.h>
//#include <stdbool.h>
#include <pthread.h>
#define N 2
typedef unsigned long int pthread_t;

int x = 0;
int token = 0;
int enter_num = -1;
int flag[N] = {0};
int thr_num_array[N*2];

int fln = 0;
int flv = 0;

int fln_ps = 0;
int flv_ps = 0;

void *thr(void* k){
	int thr_num = *((int *) k);
	//printf("thr %d start\n", thr_num);
	
	flag[thr_num] = 1;
	while(enter_num != thr_num){
		fln = thr_num;
		flv = fln;
	}
	
	// begin: critical section
	int y = 0;
	y = x;
	y++;
    x = y;
	//printf("thr %d exit from critical section, the value of x is: %d now\n", thr_num, x);
	// end: critical section
	flag[thr_num] = 0;
	
	while(1){
		fln = thr_num;
		flv = fln;
	}
	pthread_exit(NULL);
}

void *pass_ring(void* k){
	int thr_num = *((int *) k);
	//printf("pr %d start\n", thr_num);
	
	while(1){
		while (token != thr_num){
			fln_ps = thr_num;
			flv_ps = fln_ps;
		}
		
		if (flag[thr_num] == 1){
			enter_num = thr_num;
			while(flag[thr_num] == 1){
				fln_ps = thr_num;
				flv_ps = fln_ps;
			}
			enter_num = -1;
		}
		token = (token + 1) % N;
	}
	
	pthread_exit(NULL);
}

int main(){
	
	/*for (int i = 0; i < (N*2); i++){
		thr_num_array[i] = i;
	}*/
	thr_num_array[0] = 0;
	thr_num_array[1] = 1;
	thr_num_array[2] = 0;
	thr_num_array[3] = 1;
	
	pthread_t t0, p0, t1, p1;
	
	pthread_create(&t0, NULL, thr, &thr_num_array[0]);
	pthread_create(&p0, NULL, pass_ring, &thr_num_array[2]);
	pthread_create(&t1, NULL, thr, &thr_num_array[1]);
	pthread_create(&p1, NULL, pass_ring, &thr_num_array[3]);
	
	pthread_join(t0, NULL);
	pthread_join(p0, NULL);
	pthread_join(t1, NULL);
	pthread_join(p1, NULL);
	
	return 0;
}