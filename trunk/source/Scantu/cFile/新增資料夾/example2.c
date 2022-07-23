//@ ltl invariant positive: [](AP(x == 1) ==> <>AP(x == 2));

#include <pthread.h>
typedef unsigned long int pthread_t;
int x = 0;

void *thr1(void *_) {
	x = 1;
}

void *thr2(void *_) {
	x = 2;
}
  
int main() {
	pthread_t t1, t2;
	pthread_create(&t1, 0, thr1, 0);
	pthread_create(&t2, 0, thr2, 0);
	pthread_join(t1, 0);
	pthread_join(t2, 0);
	return 0;
}