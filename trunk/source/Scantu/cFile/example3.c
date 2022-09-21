//@ ltl invariant positive: <>[]AP(x == 3);
// ltl invariant positive: []AP(\at(x, L) == 2);

#include <pthread.h>
#define N 3
typedef unsigned long int pthread_t;
int x = 0;
int thr_num_array[N];

void *thr(void* k) {
	x = x + 1;
}
  
int main() {
	for (int i = 0; i < N; i++){
		thr_num_array[i] = i;
	}
	pthread_t t0, t1, t2;
	
	pthread_create(&t0, NULL, thr, &thr_num_array[0]);
	pthread_create(&t1, NULL, thr, &thr_num_array[1]);
	pthread_create(&t2, NULL, thr, &thr_num_array[2]);
	
	pthread_join(t0, NULL);
	pthread_join(t1, NULL);
	pthread_join(t2, NULL);
	
    return 0;
}