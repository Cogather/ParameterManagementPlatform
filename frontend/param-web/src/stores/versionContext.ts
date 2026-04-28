import { defineStore } from 'pinia'

export interface VersionContextState {
  versionId: string
}

export const useVersionContextStore = defineStore('versionContext', {
  state: (): VersionContextState => ({
    versionId: '',
  }),
  actions: {
    setVersionId(id: string) {
      this.versionId = id
    },
  },
})

