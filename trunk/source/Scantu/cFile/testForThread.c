// ltl invariant positive: (!(AP(fairness_label_verified == fairness_label_num) ==> []AP(fairness_label_verified == fairness_label_num)) U AP(fairness_label_end == 1)) ==> <>[]AP(x == 1);
//@ ltl invariant positive: <>[]AP(x == 2);
// ltl invariant positive: (!(<>([]AP(fairness_label == 0) || []AP(fairness_label == 1))) U AP(fairness_label == 2)) ==> <>[]AP(x == 2);

/* Testcase from Threader's distribution. For details see:
   http://www.model.in.tum.de/~popeea/research/threader
*/

#include <pthread.h>
#define N 2
typedef unsigned long int pthread_t;

int flag[2]= {0};
int turn; // integer variable to hold the ID of the thread whose turn is it
int x = 0; // boolean variable to test mutual exclusion

int thr_num_array[N];

// for fairness
int fairness_label_num = 0;
int fairness_label_verified = 0;
int fairness_label_end = 0;

void *thr(void *k) {
	int i = *((int *) k);
    flag[i] = 1;
    turn = 1 - i;
    while (flag[1-i]==1 && turn==(1-i)) {
		fairness_label_num = 0;
		fairness_label_verified = fairness_label_num;
    };
	
    // begin: critical section
    // x = 0;
	int y = 0;
	y = x;
	y++;
    x = y;
    // end: critical section
    flag[i] = 0;
	fairness_label_end++;
    pthread_exit(NULL);
}
  
int main() {
for (int i = 0; i < N; i++){
		thr_num_array[i] = i;
	}
	pthread_t t0, t1;//, t2, t3;
	
	pthread_create(&t0, NULL, thr, &thr_num_array[0]);
	pthread_create(&t1, NULL, thr, &thr_num_array[1]);
	//pthread_create(&t2, NULL, thr, &thr_num_array[2]);
	//pthread_create(&t3, NULL, thr, &thr_num_array[3]);
	
	pthread_join(t0, NULL);
	pthread_join(t1, NULL);

	return 0;
}