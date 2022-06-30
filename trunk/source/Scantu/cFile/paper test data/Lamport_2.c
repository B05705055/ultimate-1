//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 2));

#include <stdio.h>
#include <pthread.h>
#include <assert.h>
#define N 2
typedef unsigned long int pthread_t;

extern void __VERIFIER_atomic_begin();
extern void __VERIFIER_atomic_end();

int x = 0;
int number[N];
int entering[N];

int largest(int arr[], int n)
{
    int i;
    int max = arr[0];
    for (i = 1; i < n; i++)
    {
        if (arr[i] > max)
        {
           max = arr[i]; 
        }   
    }      
    return max;
}



void *thr(void* k){
        __VERIFIER_atomic_begin();
        int n = *((int *) k);
        __VERIFIER_atomic_end();
        __VERIFIER_atomic_begin();
        entering[n] = 1;
        __VERIFIER_atomic_end();
        
        number[n] = 1 + largest(number, N);
        
        __VERIFIER_atomic_begin();
        entering[n] = 0;
        __VERIFIER_atomic_end();
        for(int j = 0; j < N; j++)
        {
                while(entering[j]){}
                while ( number[j] != 0  &&
                ( number[j] < number[n] || ( number[j] == number[n] && j < n ) ) ){}
        }

        /* critical section */
        __VERIFIER_atomic_begin();
        int y = x;
        __VERIFIER_atomic_end();
        __VERIFIER_atomic_begin();
        y++;
        __VERIFIER_atomic_end();
        __VERIFIER_atomic_begin();
        x = y;
        __VERIFIER_atomic_end();
        __VERIFIER_atomic_begin();
        number[n] = 0;
        __VERIFIER_atomic_end();
        return 0;
}

int main (){
    int i[2] = {0, 1};

    pthread_t t0, t1;
    pthread_create(&t0, 0, thr, (void*) &i[0]);
    pthread_create(&t1, 0, thr, (void*) &i[1]);

    pthread_join(t0, 0);
    pthread_join(t1, 0);

    return 0;
}

