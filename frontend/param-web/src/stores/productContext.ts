import { defineStore } from 'pinia'

export interface ProductContextState {
  ownedProductId: string
  ownedProductName: string
}

export const useProductContextStore = defineStore('productContext', {
  state: (): ProductContextState => ({
    ownedProductId: '',
    ownedProductName: '',
  }),
  actions: {
    setOwnedProductId(id: string) {
      this.ownedProductId = id
    },
    setOwnedProductName(name: string) {
      this.ownedProductName = name
    },
  },
})

