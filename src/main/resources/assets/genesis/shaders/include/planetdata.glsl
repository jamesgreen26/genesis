#define NUM_PLANETS 7
#define NUM_SUNS 3

//todo: use uniform buffers

// all in viewSpace, if absolutely needed in world space literally just add the cam pos lmao

uniform vec3 planetPos[NUM_PLANETS];
uniform vec3 planetHalfSize[NUM_PLANETS];
uniform vec3 planetRot[NUM_PLANETS];
uniform float planetRoundedness[NUM_PLANETS];

uniform vec3 sunPos[NUM_SUNS];
uniform vec3 sunCol[NUM_SUNS];
uniform vec3 sunRot[NUM_SUNS];
uniform vec3 sunHalfSize[NUM_SUNS];
uniform float sunRoundedness[NUM_SUNS];