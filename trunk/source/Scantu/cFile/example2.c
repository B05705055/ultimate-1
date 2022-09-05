//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 2));
// ltl invariant positive: []AP(\at(x, L) == 2);

#include <pthread.h>
typedef unsigned long int pthread_t;
int x = 0;

void *thr1(void *_) {
	x = x + 1;
}

void *thr2(void *_) {
	x = x + 1;
}
  
int main() {
	pthread_t t1, t2;
	pthread_create(&t1, 0, thr1, 0);
	pthread_create(&t2, 0, thr2, 0);
	pthread_join(t1, 0);
	pthread_join(t2, 0);
	L: return 0;
}